package com.github.mq.constant;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class MqData implements Serializable {
    private static final long serialVersionUID = 0L;

    private String msgId;

    private String traceId;

    private Date sendTime;

    private String mqInfo;

    private String json;
}
