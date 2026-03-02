package com.leehuang.his.api.mis.dto.appointment.vo;

import com.leehuang.his.api.mis.dto.appointment.dto.GuidanceSummaryInfoVO;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.List;

@Data
public class GuidanceInfoVO {

    private GuidanceSummaryInfoVO summaryInfo;

    private String qrcodeBase64;

    private LinkedHashSet<CheckupVO> checkup;
}
