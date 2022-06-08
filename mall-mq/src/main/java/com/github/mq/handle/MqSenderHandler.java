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

    @Value("${mq.providerRetryCount:2}")
    private int maxRetryCount;

    private final RabbitTemplate rabbitTemplate;
    private final MqSendService mqSendService;

    /**
     * 用这个发送的 mq 信息, 实际发送的是 {@link MqData} 对象, 里面有「发送时间、队列信息」信息.
     * 使用 {@link MqReceiverHandler#doConsume} 处理消息
     */
    public void doProvide(MqInfo mqInfo, String searchKey, String json) {
        doProvide(mqInfo, searchKey, json, 0);
    }

    /** 用这个发送的 mq 消息, 使用 {@link MqReceiverHandler#doConsumeJustJson} 处理消息 */
    public void doProvideJustJson(MqInfo mqInfo, String searchKey, String json) {
        doProvideJustJson(mqInfo, searchKey, json, 0);
    }

    /**
     * 用这个发送的 mq 信息, 实际发送的是 {@link MqData} 对象, 里面有「发送时间、队列信息」信息.
     * 使用 {@link MqReceiverHandler#doConsume} 处理消息
     *
     * @param delayMs 延迟发送毫秒数, 需要安装 delay 插件, 见: https://www.rabbitmq.com/community-plugins.html
     */
    public void doProvide(MqInfo mqInfo, String searchKey, String json, int delayMs) {
        String msgId = U.uuid16();
        String traceId = LogUtil.getTraceId();

        MqData data = new MqData();
        data.setSendTime(new Date());
        data.setMqInfo(mqInfo.name().toLowerCase());
        data.setTraceId(traceId);
        data.setJson(json);
        provide(searchKey, new SelfCorrelationData(msgId, traceId, mqInfo, JsonUtil.toJson(data), delayMs));
    }

    /**
     * 用这个发送的 mq 消息, 使用 {@link MqReceiverHandler#doConsumeJustJson} 处理消息
     *
     * @param delayMs 延迟发送毫秒数, 需要安装 delay 插件, 见: https://www.rabbitmq.com/community-plugins.html
     */
    public void doProvideJustJson(MqInfo mqInfo, String searchKey, String json, int delayMs) {
        String msgId = U.uuid16();
        String traceId = LogUtil.getTraceId();
        provide(searchKey, new SelfCorrelationData(msgId, traceId, mqInfo, json, delayMs));
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
            model.setRetryCount(0);
            model.setMsg(json);
            model.setRemark(String.format("发送消息(%s)", desc));
            mqSendService.add(model);
        } else {
            MqSend update = new MqSend();
            update.setId(model.getId());
            update.setStatus(2);
            update.setRetryCount(model.getRetryCount() + 1);
            update.setRemark(String.format("消息(%s)重试", desc));
            mqSendService.updateById(update);
        }

        // msgId 放在 messageId, traceId 放在 correlationId
        // 默认是持久化的 setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        Message msg = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setMessageId(msgId).setCorrelationId(traceId).build();
        try {
            int delayMs = correlationData.getDelayMs();
            if (delayMs > 0) {
                rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, new DelayMessage(delayMs), correlationData);
            } else {
                rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
            }
        } catch (Exception e) {
            MqSend errorModel = new MqSend();
            errorModel.setId(model.getId());
            errorModel.setStatus(1);
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
            // 从记录中获取重试次数
            MqSend mqSend = mqSendService.queryByMsgId(correlationData.getId());
            if (U.isNotNull(mqSend)) {
                if (correlationData instanceof SelfCorrelationData data) {
                    Integer retryCount = mqSend.getRetryCount();
                    if (U.greater0(retryCount)) {
                        // 如果重试次数未达到设定的值则进行重试
                        if (retryCount < maxRetryCount) {
                            ApplicationContexts.getBean(MqSenderHandler.class).provide("", data);
                        } else {
                            mqSend.setStatus(1);
                            mqSend.setRemark(String.format("发送(%s)失败且重试(%s)达到上限(%s)",
                                    data.getMqInfo().showDesc(), retryCount, maxRetryCount));
                            mqSendService.updateById(mqSend);
                        }
                        return;
                    }
                }
                mqSend.setStatus(1);
                mqSend.setRemark("消息到交换机失败");
                mqSendService.updateById(mqSend);
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
        model.setRemark("消息到队列时失败: " + replyText);
        mqSendService.updateByMsgId(msgId, model);
    }
}
