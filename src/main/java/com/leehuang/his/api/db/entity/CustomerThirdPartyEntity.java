package com.leehuang.his.api.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_customer_third_party")
public class CustomerThirdPartyEntity {

    private Long id;

    private Integer customerId;

    private String platform;

    private String openId;

    private String nickname;

    private String avatar;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
