package com.github.mq.constant;

/**
 * <pre>
 * 一个队列有 交换机名(exchange)、路由(routing_key)、队列名(queue) 三个值
 *
 * 1. 初始 --> 绑定(binding) queue 到 exchange, 基于 routing_key
 *    交换机分为三种:
 *      + Fanout (不指定 routingKey, 所有跟它绑定的 queue 都会接收到)
 *      + Direct (全匹配 routingKey)
 *      + Topic  (模糊匹配, # 替换 0 个或多个单词, * 替换一个单词)
 *    队列可以定义死信队列(使用 { "x-dead-letter-exchange" : "交换机名",  "x-dead-letter-routing-key" : "路由键" } 设置),
 *    消费时如果出现以下三种情况, 该消息会被丢进死信(如果未配置, 消息将会被丢弃)
 *      + 消息被 channel.basicNack 或 channel.basicReject 且 requeue 的值是 false
 *      + 消息在队列的存活时间超出了设置的 ttl 时间(使用 { "x-message-ttl", 6000 } 来设置, 单位毫秒)
 *      + 消息队列的数量达到了上限
 *
 *    设置重要的消息「死信 --> 死信的死信(延迟半小时) --> 死信」成一个环, 再「重要队列 --> 死信」
 *    死信的死信因为是一个延迟队列(就是想它到期了再回去死信), 因此不需要消费, 只消费死信队列即可
 *    这样可以实现延迟队列(比如定时取消订单功能), 也可以在定义队列时设置为延迟队列(需要安装 delay 插件,
 *    见: https://www.rabbitmq.com/community-plugins.html), 在发送时设置延迟时间完成延迟功能
 *
 *   需要处理消费     需要处理消费                不需要消费
 *    example --> example_dead(死信) --> example_dead_dead(死信的死信)
 *                      ↑                         ↓
 *                      ↑                         ↓ (延迟)
 *                      ←-------------------------←
 *
 * 2. 发送 ==> 向 exchange 的 routing_key 发送消息
 *
 * 3. 消费 ==> 基于 queue
 * </pre>
 */
public class MqConst {

    /** rabbitmq 用在死信队列时的交换机名. 见: https://www.rabbitmq.com/dlx.html */
    public static final String DEAD_EXCHANGE = "x-dead-letter-exchange";
    /** rabbitmq 用在死信队列时的路由键名. 见: https://www.rabbitmq.com/dlx.html */
    public static final String DEAD_ROUTE_KEY = "x-dead-letter-routing-key";
    /** rabbitmq 用在延迟队列时的配置名. 见: https://www.rabbitmq.com/ttl.html */
    public static final String DELAY = "x-message-ttl";


    private static final String DEAD = ":dead";
    private static final String DEAD_DEAD = ":dead-dead";


    /** 交换机共用一个 */
    public static final String EXAMPLE_EXCHANGE = "example:exchange";
    /** 死信交换机共用一个 */
    public static final String DEAD_EXAMPLE_EXCHANGE = "example:exchange:dead";

    public static final String EXAMPLE_DESC = "示例";
    public static final String EXAMPLE_KEY = "example:routing-key";
    public static final String EXAMPLE_QUEUE = "example:queue";

    public static final String DEAD_EXAMPLE_DESC = "示例的死信";
    public static final String DEAD_EXAMPLE_KEY = EXAMPLE_KEY + DEAD;
    public static final String DEAD_EXAMPLE_QUEUE = EXAMPLE_QUEUE + DEAD;

    public static final String DEAD_DEAD_EXAMPLE_DESC = "示例的死信的死信";
    public static final String DEAD_DEAD_EXAMPLE_KEY = EXAMPLE_KEY + DEAD_DEAD;
    public static final String DEAD_DEAD_EXAMPLE_QUEUE = EXAMPLE_QUEUE + DEAD_DEAD;
    /** 延迟时间, 单位毫秒 */
    public static final int EXAMPLE_DEAD_DEAD_DELAY_MS = 5 * 60 * 1000;

    public static final String DELAY_EXAMPLE_DESC = "延迟队列示例";
    public static final String DELAY_EXAMPLE_EXCHANGE = "delay-example:exchange";
    public static final String DELAY_EXAMPLE_KEY = "delay-example:routing-key";
    public static final String DELAY_EXAMPLE_QUEUE = "delay-example:queue";
}
