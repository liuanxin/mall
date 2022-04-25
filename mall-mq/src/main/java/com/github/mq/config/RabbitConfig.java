package com.github.mq.config;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.mq.constant.MqInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
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

        Map<String, Exchange> exchangeMap = new LinkedHashMap<>();
        Map<String, Queue> queueMap = new LinkedHashMap<>();
        Map<String, Binding> bindingMap = new LinkedHashMap<>();
        for (MqInfo mqInfo : MqInfo.values()) {
            String exchangeType = mqInfo.getExchangeType();
            String exchangeName = mqInfo.getExchangeName();
            String routingKey = mqInfo.getRoutingKey();
            String queueName = mqInfo.getQueueName();
            String bindingName = String.format("(%s -- %s --> %s)", exchangeName, routingKey, queueName);

            Exchange exchange = exchangeMap.get(exchangeName);
            if (U.isNull(exchange)) {
                // 持久化(durable 是 true), 不自动删除(autoDelete 是 false)
                exchange = new ExchangeBuilder(exchangeName, exchangeType).build();
                exchangeMap.put(exchangeName, exchange);
            }

            Queue queue = queueMap.get(queueName);
            if (U.isNull(queue)) {
                // 持久化(durable 是 true), 不自动删除(autoDelete 是 false)
                queue = QueueBuilder.durable(queueName).withArguments(mqInfo.getArgs()).build();
                queueMap.put(queueName, queue);
            }

            if (!bindingMap.containsKey(bindingName)) {
                bindingMap.put(bindingName, BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs());
            }
        }

        for (Exchange exchange : exchangeMap.values()) {
            rabbitAdmin.declareExchange(exchange);
        }
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("declare RabbitMQ exchange({} : {})", exchangeMap.size(), exchangeMap.keySet());
        }

        for (Queue queue : queueMap.values()) {
            rabbitAdmin.declareQueue(queue);
        }
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("declare RabbitMQ queue({} : {})", queueMap.size(), queueMap.keySet());
        }

        for (Binding binding : bindingMap.values()) {
            rabbitAdmin.declareBinding(binding);
        }
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("declare RabbitMQ binding({} : {})", bindingMap.size(), bindingMap.keySet());
        }
        return rabbitAdmin;
    }
}
