package com.leehuang.his.api.front.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leehuang.his.api.db.dao.AppointmentRestrictionDao;
import com.leehuang.his.api.db.entity.AppointmentRestrictionEntity;
import com.leehuang.his.api.front.service.AppointmentRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("appointmentRestrictionServiceImpl")
@RequiredArgsConstructor
public class AppointmentRestrictionServiceImpl extends ServiceImpl<AppointmentRestrictionDao,AppointmentRestrictionEntity> implements AppointmentRestrictionService {

    private final AppointmentRestrictionDao restrictionDao;

    /**
     * 新增或更新预约表
     * @param restrictionEntity
     * @return
     */
    @Override
    public int insertOrUpdate(AppointmentRestrictionEntity restrictionEntity) {
        int rows;

        if (restrictionEntity.getId() != null) {
            rows = restrictionDao.updateById(restrictionEntity);
        } else {
            rows = restrictionDao.insert(restrictionEntity);
        }

        return rows;
    }
}
