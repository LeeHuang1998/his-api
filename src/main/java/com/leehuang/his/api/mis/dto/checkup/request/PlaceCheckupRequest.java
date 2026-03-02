package com.leehuang.his.api.mis.dto.checkup.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class PlaceCheckupRequest {

    @NotBlank(message = "uuid 不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z]{32}$", message = "uuid内容不正确")
    private String uuid;

    @NotNull(message = "placeId 不能为空")
    @Min(value = 1, message = "placeId 不能小于 1")
    private Integer placeId;

    @NotBlank(message = "科室不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z\\u4e00-\\u9fa5]{2,30}$", message = "place内容不正确")
    private String place;

}
