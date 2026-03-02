package com.leehuang.his.api.mis.dto.checkup.request;

import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultItemVO;
import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

@Data
public class CheckupResultRequest {

    @NotBlank(message = "doctorName 不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "name 内容不正确")
    private String doctorName;

    @NotBlank(message = "customerName 不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "name 内容不正确")
    private String customerName;

    @NotBlank(message = "uuid 不能为空")
    @Pattern(regexp = "^[0-9A-Za-z]{32}$", message = "uuid 内容不正确")
    private String uuid;

    @NotBlank(message = "place 不能为空")
    @Pattern(regexp = "^[0-9A-Za-z\\u4e00-\\u9fa5]{2,30}$", message = "place 内容不正确")
    private String place;

    @NotNull
    @Min(value = 1, message = "placeId 不能为空")
    private Integer placeId;

    @NotEmpty(message = "体检项目 item 不能为空")
    private List<PlaceCheckupResultItemVO> item;

    @NotEmpty(message = "template 不能为空")
    private String template;

}
