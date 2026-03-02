package com.leehuang.his.api.mis.dto.system.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SystemRequest {

    private String item;

    @NotBlank
    private String value;
}
