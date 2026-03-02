package com.leehuang.his.api.front.dto.address.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;

@Data
public class AddressRequest {

    @Null(groups = {Insert.class}, message = "id 必须为空")
    @NotNull(groups = {Update.class}, message = "id 不能为空")
    private Integer id;

    @NotBlank(message = "name 不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z·\\s]{2,30}$", message = "姓名格式不正确")
    private String name;

    @NotBlank(message = "tel 不能为空")
    private String tel;

    @NotBlank(message = "province 不能为空")
    private String province;

    private String city;

    @NotBlank(message = "district 不能为空")
    private String district;

    @NotBlank(message = "regionCode 不能为空")
    private String[] regionCode;

    @NotBlank(message = "detail 不能为空")
    private String detail;
}
