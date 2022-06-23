package com.github.mq.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;

import java.util.Map;

import static com.github.mq.constant.MqConst.*;

/**
 * <pre>
 * mq 发布订阅
 * 无需再定义 Exchange、Queue、Binding 进 spring 上下文, 也无需再使用 RabbitTemplate
 *
 * 1. 在 MqConst 中新增值: desc(队列描述）、exchangeName(交换机名)、routingKey(路由键)、queueName(队列名)
 * 2. 在 MqInfo 中新增枚举, 将上面的值对应起来
 * 3. 发布 mq 消息
 *     private final MqSenderHandler handler;
 *
 *     public void xxx() {
 *         // 发布到 mq 的数据
 *         String json = ...
 *         // 用这个方式发送到 mq 的数据, 会包括「发送时间、队列信息、traceId」等信息
 *         handler.doProvide(MqInfo.xxx, json);
 *     }
 *
 *     public void yyy() {
 *         // 发布到 mq 的数据
 *         String json = ...
 *         // 用这个方式发送到 mq 的数据, 只会将 json 的信息发过去
 *         handler.doProvideJustJson(MqInfo.xxx, json);
 *     }
 * 4. 消费 mq 消息
 *     private final MqReceiverHandler handler;
 *
 *     &#064;RabbitListener(queues = MqConst.xxx)
 *     public void xxx(Message message) {
 *         handler.doConsume(MqInfo.xxx, message, this::business);
 *     }
 *
 *     // 入参是 mq 中的数据, 返回是处理完之后需要存到表里的搜索键
 *     public String business(String json) {
 *         ...
 *         return searchKey;
 *     }
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum MqInfo {

    EXAMPLE(EXAMPLE_DESC, ExchangeTypes.DIRECT, EXAMPLE_EXCHANGE, false, EXAMPLE_KEY, EXAMPLE_QUEUE, Map.of(
            DEAD_EXCHANGE, DEAD_EXAMPLE_EXCHANGE,
            DEAD_ROUTE_KEY, DEAD_EXAMPLE_KEY
    )),
    EXAMPLE_DEAD(DEAD_EXAMPLE_DESC, ExchangeTypes.DIRECT, EXAMPLE_EXCHANGE, false, DEAD_EXAMPLE_KEY, DEAD_EXAMPLE_QUEUE, Map.of(
            DEAD_EXCHANGE, DEAD_EXAMPLE_EXCHANGE,
            DEAD_ROUTE_KEY, DEAD_DEAD_EXAMPLE_KEY
    )),
    EXAMPLE_DEAD_DEAD(DEAD_DEAD_EXAMPLE_DESC, ExchangeTypes.DIRECT, EXAMPLE_EXCHANGE, false, DEAD_DEAD_EXAMPLE_KEY, DEAD_DEAD_EXAMPLE_QUEUE, Map.of(
            DEAD_EXCHANGE, DEAD_EXAMPLE_EXCHANGE,
            DEAD_ROUTE_KEY, DEAD_EXAMPLE_KEY,
            DELAY, EXAMPLE_DEAD_DEAD_DELAY_MS
    ))
    ;

    private final String desc;
    /** @see ExchangeTypes */
    private final String exchangeType;
    private final String exchangeName;
    /** 是否是延迟交换机, 如果设置为 true 需要安装 delay 插件, 见: https://www.rabbitmq.com/community-plugins.html */
    private final boolean delayExchange;
    private final String routingKey;
    private final String queueName;
    private final Map<String, Object> args;

    public String showDesc() {
        return String.format("%s(%s -- %s --> %s)", desc, exchangeName, routingKey, queueName);
    }

    public static MqInfo from(String name) {
        for (MqInfo value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
