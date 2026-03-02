package com.leehuang.his.api.front.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leehuang.his.api.db.entity.AppointmentRestrictionEntity;

public interface AppointmentRestrictionService extends IService<AppointmentRestrictionEntity> {

    // 新增或更新
    int insertOrUpdate(AppointmentRestrictionEntity restrictionEntity);
}
