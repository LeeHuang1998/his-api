package com.leehuang.his.api.mis.dto.appointment.dto;

import lombok.Data;

@Data
public class AppointmentSnapshotDTO {

    /** tb_appointment 中的唯一 uuid **/
    private String uuid;

    private String snapshotId;
}
