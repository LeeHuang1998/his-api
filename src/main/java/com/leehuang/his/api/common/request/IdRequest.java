package com.leehuang.his.api.common.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class IdRequest {

    @NotNull(message = "ID 不能为空")
    @Min(value = 1, message = "ID 不能小于 1")
    private Integer id;
}
