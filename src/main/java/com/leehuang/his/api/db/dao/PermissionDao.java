package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.PermissionEntity;
import com.leehuang.his.api.mis.dto.role.vo.PermissionsVO;

import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_permission(权限表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.PermissionEntity
*/
public interface PermissionDao extends BaseMapper<PermissionEntity> {

    // 获取所有权限
    List<PermissionsVO> getAllPermission();
}




