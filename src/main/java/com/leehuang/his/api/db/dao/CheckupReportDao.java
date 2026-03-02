package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.CheckupReportEntity;
import org.apache.ibatis.annotations.Param;

/**
* @author 16pro
* @description 针对表【tb_checkup_report(体检报告表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.CheckupReportEntity
*/
public interface CheckupReportDao extends BaseMapper<CheckupReportEntity> {

    // 插入体检结果记录到体检报告表中
    int insert(@Param("uuid") String uuid, @Param("resultId") String resultId);
}




