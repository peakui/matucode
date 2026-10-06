package com.peakui.pay.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("pay_transaction")
public class PayTransaction {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("transaction_no")
    private String transactionNo;

    @TableField("order_no")
    private String orderNo;

    private String channel;

    @TableField("channel_trade_no")
    private String channelTradeNo;

    private BigDecimal amount;
    private Integer status;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("notify_content")
    private String notifyContent;

    @TableField("extra_info")
    private String extraInfo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
