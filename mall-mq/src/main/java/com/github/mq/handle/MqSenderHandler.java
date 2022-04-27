package com.github.mq.handle;

import com.github.common.json.JsonUtil;
import com.github.common.util.ApplicationContexts;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.mq.constant.MqData;
import com.github.mq.constant.MqInfo;
import com.github.mq.constant.SelfCorrelationData;
import com.github.mq.model.MqSend;
import com.github.mq.service.MqSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@RequiredArgsConstructor
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class MqSenderHandler implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    @Value("${mq.providerRetryCount:5}")
    private int maxRetryCount;

    private final RabbitTemplate rabbitTemplate;
    private final MqSendService mqSendService;

    /**
     * 用这个发送的 mq 信息, 实际发送的是 {@link MqData} 对象, 里面有「发送时间、队列信息」信息.
     * 使用 {@link MqReceiverHandler#doConsume} 处理消息
     */
    public void doProvide(MqInfo mqInfo, String searchKey, String json) {
        String msgId = U.uuid16();
        String traceId = LogUtil.getTraceId();
        MqData data = new MqData();
        data.setSendTime(new Date());
        data.setMqInfo(mqInfo.name().toLowerCase());
        data.setJson(json);
        provide(searchKey, new SelfCorrelationData(U.uuid16(), traceId, mqInfo, JsonUtil.toJson(data)));
    }

    /** 用这个发送的 mq 消息, 使用 {@link MqReceiverHandler#doConsumeJustJson} 处理消息 */
    public void doProvideJustJson(MqInfo mqInfo, String searchKey, String json) {
        provide(searchKey, new SelfCorrelationData(U.uuid16(), LogUtil.getTraceId(), mqInfo, json));
    }

    private void provide(String searchKey, SelfCorrelationData correlationData) {
        String msgId = correlationData.getId();
        String traceId = correlationData.getTraceId();
        MqInfo mqInfo = correlationData.getMqInfo();
        String json = correlationData.getJson();

        String exchangeName = mqInfo.getExchangeName();
        String routingKey = mqInfo.getRoutingKey();
        String desc = mqInfo.showDesc();

        MqSend model = mqSendService.queryByMsgId(msgId);
        if (U.isNull(model)) {
            model = new MqSend();
            model.setMsgId(msgId);
            model.setSearchKey(searchKey);
            model.setBusinessType(mqInfo.name().toLowerCase());
            // 先写成功, 后面异常时写失败
            model.setStatus(2);
            model.setFailType(0);
            model.setRetryCount(0);
            model.setMsg(json);
            model.setRemark(String.format("发送消息(%s)", desc));
            mqSendService.add(model);
        } else {
            MqSend update = new MqSend();
            update.setId(model.getId());
            update.setStatus(2);
            update.setFailType(0);
            update.setRetryCount(model.getRetryCount() + 1);
            update.setRemark(String.format("消息(%s)重试", desc));
            mqSendService.updateById(update);
        }

        // msgId 放在 messageId, traceId 放在 correlationId
        // 默认是持久化的 setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        Message msg = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setMessageId(msgId).setCorrelationId(traceId).build();
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
        } catch (Exception e) {
            MqSend errorModel = new MqSend();
            errorModel.setId(model.getId());
            errorModel.setStatus(1);
            errorModel.setFailType(1);
            errorModel.setRemark(String.format("连接 mq 失败(%s)", desc));
            mqSendService.updateById(errorModel);
        }
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("消息({})到 exchange 成功", JsonUtil.toJson(correlationData));
            }
        } else {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消息({})到 exchange 失败, 原因({})", JsonUtil.toJson(correlationData), cause);
            }
            if (correlationData instanceof SelfCorrelationData) {
                SelfCorrelationData data = (SelfCorrelationData) correlationData;

                // 从记录中获取重试次数
                MqSend mqSend = mqSendService.queryByMsgId(correlationData.getId());
                if (U.isNotNull(mqSend) && U.greater0(mqSend.getRetryCount())) {
                    if (mqSend.getRetryCount() < maxRetryCount) {
                        // 重试
                        ApplicationContexts.getBean(MqSenderHandler.class).provide("", data);
                    } else {
                        mqSend.setStatus(1);
                        mqSend.setFailType(2);
                        mqSend.setRemark(String.format("%s发送失败且重试(%s)达到上限", data.getMqInfo().showDesc(), maxRetryCount));
                        mqSendService.updateById(mqSend);
                    }
                }
            }
        }
    }

    /*
    发布消息, rabbitmq 的投递路径是:
      producer -> exchange -- (routing_key) --> queue -> consumer

    ConfirmCallback#confirm        : 消息跟 exchange 交互时触发回调, 成功到达则 ack 为 true, 此时还无法确定消息有没有到达 queue
    ReturnCallback#returnedMessage : 消息 routing 不到 queue 时触发回调(需要设置 spring.rabbitmq.template.mandatory = true, 默认会将消息丢弃)
    */

    @Override
    public void returnedMessage(ReturnedMessage msg) {
        Message message = msg.getMessage();
        String replyText = msg.getReplyText();
        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
            LogUtil.ROOT_LOG.error("消息({})无法到队列: 响应码({}) 响应文本({}) 交换机({}) 路由键({})",
                    JsonUtil.toJson(message), msg.getReplyCode(), replyText, msg.getExchange(), msg.getRoutingKey());
        }
        String msgId = message.getMessageProperties().getMessageId();

        MqSend model = new MqSend();
        model.setStatus(1);
        model.setFailType(3);
        model.setRemark(replyText);
        mqSendService.updateByMsgId(msgId, model);
    }
}
