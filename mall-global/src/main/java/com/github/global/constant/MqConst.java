package com.github.global.constant;

/**
 * <pre>
 * 一个队列有 交换机名(exchange)、路由(routing_key)、队列名(queue) 三个值
 *
 * 1. 初始 --> 绑定(binding) queue 到 exchange, 基于 routing_key
 *    交换机分为三种:
 *      + Fanout(不指定 routingKey, 所有跟它绑定的 queue 都会接收到)
 *      + Direct(全匹配 routingKey)
 *      + Topic(模糊匹配, # 匹配一个或多个, * 匹配一个)
 *    队列可以定义死信队列, 消费时如果出现以下三种情况, 该消息会被丢进死信(未配置消息将会被丢弃)
 *      + 消息被 channel.basicNack 或 channel.basicReject 且 requeue 的值是 false
 *      + 消息在队列的存活时间超出了设置的 ttl 时间
 *      + 消息队列的数量达到了上限
 *    设置重要的消息「死信 --> 死信的死信(延迟半小时) --> 死信」成一个环, 再「重要队列 --> 死信」
 *    死信的死信因为是一个延迟队列(就是想它到期了再回去死信), 因此不需要消费, 只消费死信队列即可
 *
 * 2. 发送 ==> 向 exchange 的 routing_key 发送消息
 *
 * 3. 消费 ==> 基于 queue
 * </pre>
 */
public class MqConst {

    public static final String EXAMPLE_EXCHANGE = "example:exchange";

    public static final String EXAMPLE_DESC = "示例";
    public static final String EXAMPLE_ROUTING_KEY = "example:routing-key";
    public static final String EXAMPLE_QUEUE = "example:queue";
}
