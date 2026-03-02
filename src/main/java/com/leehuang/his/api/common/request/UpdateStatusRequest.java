package com.leehuang.his.api.common.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "id 不能为空")
    @Min(value = 1, message = "id 最小为 1")
    private Integer id;

    @NotNull(message = "status 不能为空")
    private Boolean status;
}
