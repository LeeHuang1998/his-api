package com.leehuang.his.api.mis.dto.goods.request;

import com.leehuang.his.api.mis.dto.goods.validation.ImagesPattern;
import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupItemVo;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsRequest {
    @Null(groups = {Insert.class}, message = "id 必须为空")
    @NotNull(groups = {Update.class}, message = "id 不能为空")
    private Integer id;

    @NotBlank(message = "title 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,50}$", message = "title 内容不正确")
    private String title;

    @NotBlank(message = "code 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "code 内容不正确")
    private String code;

    @NotBlank(message = "description 不能为空")
    @Length(max = 200, message = "description 不能超过 200 个字符")
    private String description;

    @NotNull(message = "initialPrice 不能为空")
    @Min(value = 0, message = "initialPrice 不能小于 0")
    private BigDecimal initialPrice;

    @NotNull(message = "currentPrice 不能为空")
    @Min(value = 0, message = "currentPrice 不能小于 0")
    private BigDecimal currentPrice;

    @Min(value = 1, message = "ruleId 不能小于 1")
    private Integer ruleId;

    @NotEmpty(message = "images 不能为空")
    @Size(min = 1, max = 5, message = "images 最小为 1， 最大为 5")
    @ImagesPattern(regexp = "^[0-9a-zA-Z\\-_/\\.]{1,300}$", message = "图片路径格式不正确")
    private String[] images;

    @NotBlank(message = "type不能为空")
    @Pattern(regexp = "^父母体检$|^入职体检$|^职场白领$|^个人高端$|^中青年体检$")
    private String type;

    private String[] tags;

    @Range(min = 1, max = 5, message = "partId 不能小于 1 或大于 5")
    private Integer partId;

    // 四种检查类型 @Valid 表示嵌套验证，该属性没有声明验证规则，而是启用嵌套对象的验证，Spring 的验证器会递归地验证这个列表中的每一个 CheckupVo 实例
    // 即使外层对象的字段验证通过，若嵌套对象的验证失败，整个验证也会失败
    // 如果去掉 @Valid，即使 CheckupVo 内部有验证注解，也不会被触发
    @Valid
    private List<CheckupItemVo> checkup_1;              // 科室检查项目

    @Valid
    private List<CheckupItemVo> checkup_2;              // 实验室检查

    @Valid
    private List<CheckupItemVo> checkup_3;              // 医技检查

    @Valid
    private List<CheckupItemVo> checkup_4;              // 其他检查
}
