package com.leehuang.his.api.common.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class IdsRequest {
    @NotEmpty(message = "ids 不能为空")
    private Integer[] ids;
}
