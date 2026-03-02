package com.leehuang.his.api.front.dto.address.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AddressStatusRequest {

    @NotNull(message = "id 不能为空")
    private Integer id;

    @NotNull(message = "isDefault 不能为空")
    private Boolean isDefault;
}
