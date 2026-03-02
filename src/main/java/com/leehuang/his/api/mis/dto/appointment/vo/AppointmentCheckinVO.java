package com.leehuang.his.api.mis.dto.appointment.vo;

import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;
import lombok.Data;

@Data
public class AppointmentCheckinVO {

    private boolean checkinResult;

    private NextPlaceVO nextPlaceVO;
}
