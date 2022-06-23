
### mq 发布订阅

无需再定义 Exchange、Queue、Binding 进 spring 上下文, 也无需再使用 RabbitTemplate

1. 在 MqConst 中新增值: desc(队列描述）、exchangeName(交换机名)、routingKey(路由键)、queueName(队列名)
2. 在 MqInfo 中新增枚举, 将上面的值对应起来
3. 发布 mq 消息
```java
private final MqSenderHandler handler;

public void xxx() {
    // 发布到 mq 的数据
    String json = ...
    // 用这个方式发送到 mq 的数据, 会包括「发送时间、队列信息、traceId」等信息
    handler.doProvide(MqInfo.xxx, json);
}

public void yyy() {
    // 发布到 mq 的数据
    String json = ...
    // 用这个方式发送到 mq 的数据, 只会将 json 的信息发过去
    handler.doProvideJustJson(MqInfo.xxx, json);
}
```
4. 消费 mq 消息
```java
private final MqReceiverHandler handler;

@RabbitListener(queues = MqConst.xxx)
public void onReceive(Message message) {
    handler.doConsume(MqInfo.xxx, message, this::business);
}

/** 入参是 mq 中的数据, 返回是处理完之后需要存到表里的搜索键 */
public void business(String json) {
    // ...
    return searchKey;
}
```
