package com.leehuang.his.api.mis.dto.appointment.vo;

import lombok.Data;

import java.util.List;

@Data
public class ContinueCheckupVO {

    private CustomerSummaryInfoVO summaryInfoVO;

    private List<CheckupCompletedInfoVO> checkupPlace;
}
