package com.leehuang.his.api.mis.dto.goods.request;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class GoodsPageRequest {

    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{1,50}$", message = "keyword 内容不正确")
    private String keyword;

    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "code 内容不正确")
    private String code;

    @Pattern(regexp = "^父母体检$|^入职体检$|^职场白领$|^个人高端$|^中青年体检$", message = "type 内容不正确")
    private String type;

    @Range(min = 1, max = 5, message = "partId 范围不正确")
    private Integer partId;

    private Boolean status;

    @NotNull(message = "page 页码不能为空")
    @Min(value = 1, message = "page 页码不能小于 1")
    private Integer page;

    @NotNull(message = "length 每页条数不能为空")
    @Range(min = 10, max = 50, message = "length 每页条数不能小于 10 大于 50")
    private Integer length;
}

