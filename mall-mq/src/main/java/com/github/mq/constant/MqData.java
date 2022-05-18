package com.github.mq.constant;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 实际发送到 mq 的数据格式 */
@Data
public class MqData implements Serializable {
    private static final long serialVersionUID = 0L;

    private Date sendTime;

    private String mqInfo;

    private String traceId;

    private String json;
}
