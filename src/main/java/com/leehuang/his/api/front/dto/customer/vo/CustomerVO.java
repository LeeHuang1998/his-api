package com.leehuang.his.api.front.dto.customer.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.leehuang.his.api.front.dto.address.vo.AddressVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerVO {

    private Integer id;

    private String username;

    private String name;

    private String sex;

    private String tel;

    private String email;

    private String photo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<AddressVO> addressList;
}
