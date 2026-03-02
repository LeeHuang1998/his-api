package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.RoleEntity;
import com.leehuang.his.api.mis.dto.role.vo.RolePageVO;
import com.leehuang.his.api.mis.dto.role.vo.PermissionsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_role(角色表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.RoleEntity
*/
public interface RoleDao extends BaseMapper<RoleEntity> {



    // 获取所有角色
    List<RoleEntity> getAllRoles();

    // 获取角色数量
    int getRoleCount(@Param("roleName") String roleName);

    // 根据分页条件查询角色数据
    List<RolePageVO> getRoleDataByPage(@Param("start") int start, @Param("length") int length, @Param("roleName") String roleName);

    int insertRole(RoleEntity roleEntity);

    // 根据角色 id 获取权限
    List<PermissionsVO> getRolePermissionById(@Param("id") Integer id);

    // 根据角色 id 获取角色信息
    RoleEntity getRoleById(@Param("id") Integer id);

    // 修改角色信息
    int updateRole(RoleEntity roleEntity);

    // 根据角色 id 查询拥有该角色的用户
    List<Integer> getUserIdByRoleId(Integer roleId);

    // 查询是否可以删除
    boolean selectCanDelRole(@Param("ids") Integer[] ids);

    // 删除角色
    int deleteRole(@Param("ids") Integer[] ids);
}




