package com.leehuang.his.api.mis.dto.banner.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Data
public class BannerRequest {

    @Null(groups = {Insert.class}, message = "bannerId 必须为空")
    @NotNull(groups = {Update.class}, message = "bannerId 不能为空")
    @Min(groups = {Update.class}, value = 1,message = "bannerId 必须大于 0")
    private Integer id;

    @NotBlank(message = "轮播图名称不能为空")
    private String name;

    @NotNull(message = "goodsId 不能为空")
    @Min(value = 1,message = "goodsId 必须大于 0")
    private Integer goodsId;

    @Length(max = 50, message = "备注不能超过 50 个字符")
    private String remarks;

    @NotBlank(message = "图片不能为空")
    private String image;

    @NotNull(message = "status 不能为空")
    private Boolean status;
}
