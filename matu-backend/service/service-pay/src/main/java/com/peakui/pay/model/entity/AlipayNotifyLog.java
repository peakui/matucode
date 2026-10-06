package com.peakui.pay.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("alipay_notify_log")
public class AlipayNotifyLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("notify_id")
    private String notifyId;

    @TableField("trade_no")
    private String tradeNo;

    @TableField("out_trade_no")
    private String outTradeNo;

    @TableField("raw_body")
    private String rawBody;

    @TableField("sign_verified")
    private Integer signVerified;

    private Integer processed;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
