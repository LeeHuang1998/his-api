package com.leehuang.his.api.front.dto.goods.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SearchGoodsSnapshotByIdRequest {


    @NotBlank(message = "snapshotId不能为空")
    @Pattern(regexp = "^[0-9a-z]{24}$", message = "snapshotId内容不正确")
    private String snapshotId;

}
