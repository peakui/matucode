package com.peakui.pay.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("pay_refund")
public class PayRefund {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("refund_no")
    private String refundNo;

    @TableField("transaction_no")
    private String transactionNo;

    @TableField("order_no")
    private String orderNo;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    private String reason;
    private Integer status;

    @TableField("channel_refund_no")
    private String channelRefundNo;

    @TableField("refund_time")
    private LocalDateTime refundTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
