package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.RoleEntity;
import com.leehuang.his.api.mis.dto.role.request.RolePageRequest;
import com.leehuang.his.api.mis.dto.role.request.RoleRequest;
import com.leehuang.his.api.mis.dto.role.vo.RolePageVO;
import com.leehuang.his.api.mis.dto.role.vo.RoleDetailVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleService {

    // 查询所有角色
    List<RoleEntity> getAllRoles();

    // 分页查询角色数据
    PageUtils<RolePageVO> getRoleListByPage(RolePageRequest request);

    // 新增角色
    int insertRole(RoleRequest request);

    // 根据 id 查询角色
    RoleDetailVO getRoleById(Integer id);

    // 修改角色信息
    int updateRole(RoleRequest request);

    // 根据角色 id 查询拥有该角色的用户
    List<Integer> getUserIdByRoleId(@Param("roleId") Integer roleId);

    // 删除角色
    int deleteRole(Integer[] ids);
}
