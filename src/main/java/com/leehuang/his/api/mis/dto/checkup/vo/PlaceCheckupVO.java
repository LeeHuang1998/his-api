package com.leehuang.his.api.mis.dto.checkup.vo;

import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import lombok.Data;

import java.util.List;

@Data
public class PlaceCheckupVO {

    private List<CheckupVO> checkupVOList;

    private boolean hasAlreadyCheckup;

    private List<PlaceCheckupResultVO> checkupResultList;
}
