package com.leehuang.his.api.front.dto.goods.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * GoodsList 页面分页查询
 */
@Data
public class GoodsListPageRequest {

    @Length(min = 1, max = 50, message = "keyword 字数超出范围")
    private String keyword;

    @Min(value = 1, message = "partId 不能小于 1")
    private Integer partId;

    @Pattern(regexp = "^父母体检$|^入职体检$|^职场白领$|^个人高端$|^中青年体检$",
            message = "type内容不正确")
    private String type;

    @Pattern(regexp = "^男性$|^女性$")
    private String sex;

    @Range(min = 1, max = 4, message = "priceType 范围不正确")
    private Integer priceType;

    @Range(min = 1, max = 4, message = "orderType 范围不正确")
    private Integer orderType;

    @NotNull(message = "page 不能为空")
    @Min(value = 1, message = "page 不能小于 1 ")
    private Integer page;

    @NotNull(message = "length 不能为空")
    @Range(min = 10, max = 50, message = "length 必须为 10~50 之间")
    private Integer length;
}
