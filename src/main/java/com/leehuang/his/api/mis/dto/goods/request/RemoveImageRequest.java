package com.leehuang.his.api.mis.dto.goods.request;

import com.leehuang.his.api.mis.dto.goods.validation.ImagesPattern;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class RemoveImageRequest {

    @NotEmpty(message = "images 不能为空")
    @ImagesPattern(regexp = "^[0-9a-zA-Z\\-_/\\.]{1,300}$", message = "图片路径格式不正确")
    private String[] removeImages;
}
