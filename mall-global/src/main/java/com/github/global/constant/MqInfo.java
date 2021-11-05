package com.github.global.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

import static com.github.global.constant.MqConst.*;

@Getter
@AllArgsConstructor
public enum MqInfo {

    EXAMPLE(EXAMPLE_DESC, EXAMPLE_EXCHANGE, EXAMPLE_ROUTING_KEY, EXAMPLE_QUEUE, Collections.emptyMap())
    ;

    private final String desc;
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;
    private final Map<String, Object> args;

    public String providerDesc() {
        return String.format("%s(%s -- %s --> %s)", desc, exchangeName, routingKey, queueName);
    }
    public String consumerDesc() {
        return String.format("%s(%s <-- %s -- %s)", desc, queueName, routingKey, exchangeName);
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
