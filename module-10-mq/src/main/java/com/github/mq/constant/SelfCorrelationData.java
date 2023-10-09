package com.github.mq.constant;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SelfCorrelationData extends CorrelationData {

    private String traceId;
    private MqInfo mqInfo;
    private String json;
    private int delayMs;

    public SelfCorrelationData(String msgId, String traceId, MqInfo mqInfo, String json, int delayMs) {
        super(msgId);

        this.traceId = traceId;
        this.mqInfo = mqInfo;
        this.json = json;
        this.delayMs = delayMs;
    }
}
