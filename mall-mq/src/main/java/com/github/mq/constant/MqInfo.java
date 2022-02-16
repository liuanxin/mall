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
 *         // 发布到 mq 的实体
 *         XXX&lt;YYY&gt; req = ...;
 *         handler.doProvide(MqInfo.xxx, JsonUtil.toJson(req));
 *     }
 * 4. 消费 mq 消息
 *     private final MqReceiverHandler handler;
 *     private final XxxService xxxService;
 *
 *     &#064;RabbitListener(queues = MqConst.xxx)
 *     public void onReceive(Message message, Channel channel) {
 *         handler.doConsume(message, channel, this::business);
 *     }
 *     public void business(String json) {
 *         XXX&lt;YYY&gt; req = JsonUtil.convertType(json, new TypeReference&lt;XXX&lt;YYY&gt;&gt;() {});
 *         // 从 mq 接收到的实体
 *         if (req != null) {
 *             xxxService.xxx(req);
 *         }
 *     }
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum MqInfo {

    EXAMPLE(EXAMPLE_DESC, ExchangeTypes.DIRECT, EXAMPLE_EXCHANGE, EXAMPLE_KEY, EXAMPLE_QUEUE, Map.of(
            DEAD_EXCHANGE, DEAD_EXAMPLE_EXCHANGE,
            DEAD_ROUTE_KEY, DEAD_EXAMPLE_KEY
    )),
    EXAMPLE_DEAD(DEAD_EXAMPLE_DESC, ExchangeTypes.DIRECT, EXAMPLE_EXCHANGE, DEAD_EXAMPLE_KEY, DEAD_EXAMPLE_QUEUE, Map.of(
            DEAD_EXCHANGE, DEAD_EXAMPLE_EXCHANGE,
            DEAD_ROUTE_KEY, DEAD_DEAD_EXAMPLE_KEY
    )),
    EXAMPLE_DEAD_DEAD(DEAD_DEAD_EXAMPLE_DESC, ExchangeTypes.DIRECT, EXAMPLE_EXCHANGE, DEAD_DEAD_EXAMPLE_KEY, DEAD_DEAD_EXAMPLE_QUEUE, Map.of(
            DEAD_EXCHANGE, DEAD_EXAMPLE_EXCHANGE,
            DEAD_ROUTE_KEY, DEAD_EXAMPLE_KEY,
            DELAY, EXAMPLE_DEAD_DEAD_DELAY_MS
    ))
    ;

    private final String desc;
    private final String exchangeType;
    private final String exchangeName;
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
