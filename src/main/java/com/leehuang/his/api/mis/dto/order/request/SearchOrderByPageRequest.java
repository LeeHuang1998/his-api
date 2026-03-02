package com.leehuang.his.api.mis.dto.order.request;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class SearchOrderByPageRequest {


    @Pattern(regexp = "^1[1-9]\\d{9}$", message = "tel 内容不正确")
    private String tel;

    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "code 内容不正确")
    private String code;

    @Pattern(regexp = "^[a-zA-Z0-9]{6,64}$", message = "outTradeNo 内容不正确")
    private String outTradeNo;

    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{1,50}$", message = "keyword 内容不正确")
    private String keyword;

    @Range(min = 1, max = 6, message = "status 内容不正确")
    private Integer status;

    @Pattern(regexp = "^((((1[6-9]|[2-9]\\d)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((1[6-9]|[2-9]\\d)\\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|(((1[6-9]|[2-9]\\d)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29))$", message = "startDate内容不正确")
    private String startDate;

    @Pattern(regexp = "^((((1[6-9]|[2-9]\\d)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((1[6-9]|[2-9]\\d)\\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|(((1[6-9]|[2-9]\\d)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29))$", message = "endDate内容不正确")
    private String endDate;

    @NotNull(message = "page 不能为空")
    @Min(value = 1, message = "page 不能小于1")
    private Integer page;

    @NotNull(message = "length 不能为空")
    @Range(min = 5, max = 50, message = "length 必须为 10~50 之间")
    private Integer length;

    private Integer customerId;
}
