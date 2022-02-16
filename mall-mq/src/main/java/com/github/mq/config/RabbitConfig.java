package com.github.mq.config;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.mq.constant.MqInfo;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnClass(RabbitAdmin.class)
@RequiredArgsConstructor
public class RabbitConfig {

    /** @see org.springframework.boot.autoconfigure.amqp.RabbitProperties */
    private final ConnectionFactory connectionFactory;

    @Bean
    public RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        Map<String, Exchange> exchangeMap = Maps.newHashMap();
        Map<String, Queue> queueMap = Maps.newHashMap();
        Map<String, Binding> bindingMap = Maps.newHashMap();
        for (MqInfo mqInfo : MqInfo.values()) {
            String exchangeName = mqInfo.getExchangeName();
            String routingKey = mqInfo.getRoutingKey();
            String exchangeType = mqInfo.getExchangeType();
            String queueName = mqInfo.getQueueName();
            String bindingName = String.format("%s(%s) --> %s", exchangeName, routingKey, queueName);

            Exchange exchange = exchangeMap.get(exchangeName);
            if (U.isNull(exchange)) {
                // 默认持久化(durable 是 true), 不自动删除(autoDelete 是 false)
                exchange = new ExchangeBuilder(exchangeName, exchangeType).build();
                exchangeMap.put(exchangeName, exchange);
            }

            Queue queue = queueMap.get(queueName);
            if (U.isNull(queue)) {
                // 持久化, 默认不自动删除(autoDelete 是 false)
                queue = QueueBuilder.durable(queueName).withArguments(mqInfo.getArgs()).build();
                queueMap.put(queueName, queue);
            }

            Binding binding = bindingMap.get(bindingName);
            if (U.isNull(binding)) {
                binding = BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
                bindingMap.put(bindingName, binding);
            }
        }

        for (Exchange exchange : exchangeMap.values()) {
            rabbitAdmin.declareExchange(exchange);
        }
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("定义 RabbitMQ 路由({} 个: {})", exchangeMap.size(), exchangeMap.keySet());
        }

        for (Queue queue : queueMap.values()) {
            rabbitAdmin.declareQueue(queue);
        }
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("定义 RabbitMQ 队列({} 个: {})", queueMap.size(), queueMap.keySet());
        }

        for (Binding binding : bindingMap.values()) {
            rabbitAdmin.declareBinding(binding);
        }
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("定义 RabbitMQ 绑定({} 个: {})", bindingMap.size(), bindingMap.keySet());
        }
        return rabbitAdmin;
    }
}
