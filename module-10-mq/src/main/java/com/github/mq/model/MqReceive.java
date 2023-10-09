package com.github.mq.model;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** mq 消费记录 --> t_mq_receive */
@Data
@Table("t_mq_receive")
public class MqReceive implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 消息 id --> msg_id */
    private String msgId;

    /** 搜索键 --> search_key */
    private String searchKey;

    /** 业务场景 --> type */
    @Column("`type`")
    private String type;

    /** 0.初始, 1.失败, 2.成功(需要重试则改为 1) --> status */
    private Integer status;

    /** 重试次数(需要重试则改为 0) --> retry_count */
    private Integer retryCount;

    /** 消息内容 --> msg */
    private String msg;

    /** 备注 --> remark */
    private String remark;

    /** 创建时间 --> create_time */
    private Date createTime;

    /** 更新时间 --> update_time */
    private Date updateTime;
}
