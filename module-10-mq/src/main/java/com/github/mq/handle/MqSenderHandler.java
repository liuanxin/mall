package com.github.mq.handle;

import com.github.common.date.Dates;
import com.github.common.json.JsonUtil;
import com.github.common.util.ApplicationContexts;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.mq.constant.MqConst;
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

    @Value("${mq.provider-retry-count:2}")
    private int providerRetryCount;

    private final RabbitTemplate rabbitTemplate;
    private final MqSendService mqSendService;

    /**
     * 用这个发送的 mq 信息, 实际发送的是 {@link MqData} 对象, 里面有「发送时间、队列信息」信息
     *
     * @param searchKey 保存到表中用来做搜索的值, 比如单号等
     */
    public void doProvide(MqInfo mqInfo, String searchKey, String json) {
        doProvide(mqInfo, searchKey, json, 0);
    }

    /**
     * 用这个发送的 mq 信息, 实际发送的是 {@link MqData} 对象, 里面有「发送时间、队列信息」信息
     *
     * @param delayMs 延迟发送毫秒数, 需要安装 <a href="https://www.rabbitmq.com/community-plugins.html">delay</a> 插件
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

    /** 发送 mq 消息, 不包「发送时间、队列信息」这些内容 */
    public void doProvideJustJson(MqInfo mqInfo, String searchKey, String json) {
        provide(searchKey, new SelfCorrelationData(U.uuid16(), LogUtil.getTraceId(), mqInfo, json, 0));
    }

    /**
     * 发送 mq 消息, 不包「发送时间、队列信息」这些内容
     *
     * @param delayMs 延迟发送毫秒数, 需要安装 <a href="https://www.rabbitmq.com/community-plugins.html">delay</a> 插件
     */
    public void doProvideJustJson(MqInfo mqInfo, String searchKey, String json, int delayMs) {
        provide(searchKey, new SelfCorrelationData(U.uuid16(), LogUtil.getTraceId(), mqInfo, json, delayMs));
    }

    /** 指定 msgId 发送 mq 消息(发送的 mq 消息不包「发送时间、队列信息」这些内容), 一般用于重试 */
    public void doProvideJustJson(String msgId, MqInfo mqInfo, String searchKey, String json) {
        provide(searchKey, new SelfCorrelationData(msgId, LogUtil.getTraceId(), mqInfo, json, 0));
    }

    private void provide(String searchKey, SelfCorrelationData correlationData) {
        String msgId = correlationData.getId();
        String traceId = correlationData.getTraceId();
        MqInfo mqInfo = correlationData.getMqInfo();
        String json = correlationData.getJson();
        int delayMs = correlationData.getDelayMs();

        String exchangeName = mqInfo.getExchangeName();
        String routingKey = mqInfo.getRoutingKey();
        String desc = mqInfo.showDesc();

        MqSend model = null;
        boolean needAdd = false;
        String remark = U.EMPTY;
        int status = MqConst.INIT;
        try {
            model = mqSendService.queryByMsgId(msgId);
            needAdd = U.isNull(model);
            if (needAdd) {
                model = new MqSend();
                model.setMsgId(msgId);
                if (U.isNotBlank(searchKey)) {
                    model.setSearchKey(U.toStr(searchKey));
                }
                model.setType(mqInfo.name().toLowerCase());
                model.setRetryCount(0);
                model.setMsg(json);
            }

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("开始发送 {} 数据({})", desc, json);
            }
            // msgId 放在 messageId, traceId 放在 correlationId
            // 默认是持久化的 setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            Message msg = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                    .setMessageId(msgId).setCorrelationId(traceId).build();
            if (delayMs > 0) {
                rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, new DelayMessage(delayMs), correlationData);
            } else {
                rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
            }
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("发送({})数据({})成功", desc, msgId);
            }
            status = MqConst.SUCCESS;
            remark = String.format("<%s : 消息(%s)发送成功>%s", Dates.nowDateTime(), desc, U.toStr(model.getRemark()));
        } catch (RuntimeException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("发送({})数据({})异常", desc, msgId, e);
            }
            status = MqConst.FAIL;
            String oldRemark = U.toStr(U.isNull(model) ? null : model.getRemark());
            remark = String.format("<%s : 发送(%s)数据异常(%s)>%s", Dates.nowDateTime(),
                    desc, e.getMessage(), oldRemark);
            throw e;
        } finally {
            if (U.isNotNull(model)) {
                if (needAdd) {
                    model.setStatus(status);
                    model.setRemark(remark);
                    mqSendService.add(model);
                } else {
                    MqSend update = new MqSend();
                    update.setId(model.getId());
                    update.setStatus(status);
                    update.setRemark(remark);
                    update.setRetryCount(model.getRetryCount() + 1);
                    mqSendService.updateById(update);
                }
            }
        }
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("消息({})到交换机成功", JsonUtil.toJson(correlationData));
            }
        } else {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消息({})到交换机失败, 原因({})", JsonUtil.toJson(correlationData), cause);
            }
            String msgId = correlationData.getId();
            if (U.isNotBlank(msgId)) {
                MqSend mqSend = mqSendService.queryByMsgId(msgId);
                if (U.isNotNull(mqSend)) {
                    if (correlationData instanceof SelfCorrelationData data) {
                        int retryCount = U.toInt(mqSend.getRetryCount());
                        // 如果重试次数未达到设定的值则进行重试
                        if (retryCount < providerRetryCount) {
                            ApplicationContexts.getBean(MqSenderHandler.class).provide(null, data);
                        } else {
                            String remark = String.format("<%s : 发送失败且重试(%s)达到上限(%s)>",
                                    Dates.nowDateTime(), retryCount, providerRetryCount);
                            handleError(mqSend, remark);
                        }
                    } else {
                        String remark = String.format("<%s : 消息到交换机失败>", Dates.nowDateTime());
                        handleError(mqSend, remark);
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

    @SuppressWarnings("NullableProblems")
    @Override
    public void returnedMessage(ReturnedMessage msg) {
        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
            LogUtil.ROOT_LOG.error("消息({})到队列失败", JsonUtil.toJson(msg));
        }
        // msgId 放在 messageId, traceId 放在 correlationId
        String msgId = msg.getMessage().getMessageProperties().getMessageId();
        if (U.isNotBlank(msgId)) {
            MqSend mqSend = mqSendService.queryByMsgId(msgId);
            if (U.isNotNull(mqSend)) {
                int code = msg.getReplyCode();
                String text = msg.getReplyText();
                String remark = String.format("<%s : 消息到队列时失败(%s -> %s)>", Dates.nowDateTime(), code, text);
                handleError(mqSend, remark);
            }
        }
    }

    private void handleError(MqSend mqSend, String remark) {
        MqSend model = new MqSend();
        model.setId(mqSend.getId());
        model.setStatus(MqConst.FAIL);
        model.setRemark(remark + U.toStr(mqSend.getRemark()));
        mqSendService.updateById(model);
    }
}
