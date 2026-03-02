package com.leehuang.his.api.db.pojo;

import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultVO;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document("checkup_result")
public class CheckupResultEntity {
    @Id
    private String _id;

    /** tb_appointment 中的唯一 uuid **/
    @Indexed
    private String uuid;

    /** 体检人姓名 **/
    private String name;

    /** 体检签到日期 **/
    private LocalDateTime checkinDate;

    /** 本次体检的所有体检项 **/
    private List<CheckupVO> checkup;

    /** 体检科室 **/
    private List<String> place;

    /** 体检结果，根据科室，该科室体检项结果相关信息 **/
    private List<PlaceCheckupResultVO> result;

    /** 体检是否已完成 **/
    private Boolean completed;
}
