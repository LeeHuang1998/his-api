package com.leehuang.his.api.mis.dto.customerIm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MisCustomerInfoVO {

    private Integer id;

    private String username;

    private String name;

    private String sex;

    private String tel;

    private String photo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private BigDecimal totalAmount;

    private Long totalCount;

    private Integer number;
}
