package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.RoleEntity;
import com.leehuang.his.api.db.dao.RoleDao;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.role.request.RolePageRequest;
import com.leehuang.his.api.mis.dto.role.request.RoleRequest;
import com.leehuang.his.api.mis.dto.role.vo.RolePageVO;
import com.leehuang.his.api.mis.dto.role.vo.RoleDetailVO;
import com.leehuang.his.api.mis.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service("roleService")
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleDao roleDao;


    /**
     * 获取所有角色
     * @return  返回角色 List
     */
    @Override
    public List<RoleEntity> getAllRoles() {
        return roleDao.getAllRoles();
    }

    /**
     * 根据分页获取角色列表
     * @param request    分页查询请求参数
     * @return           返回分页查询结果，封装到 pageUtils 对象中
     */
    @Override
    public PageUtils<RolePageVO> getRoleListByPage(RolePageRequest request) {

        List<RolePageVO> roleList = new ArrayList<>();

        Integer length = request.getLength();
        int start = (request.getPage() -1) * length;

        String roleName = request.getRoleName();

        // 获取 total
        int total = roleDao.getRoleCount(roleName);
        if (total != 0){
            // 查询角色数据
            roleList = roleDao.getRoleDataByPage(start, length, roleName);
        }

        return new PageUtils<>(total, length, start, roleList);
    }

    /**
     * 插入新角色
     * @return  返回插入的行数
     */
    @Override
    @Transactional
    public int insertRole(RoleRequest request) {
        RoleEntity roleEntity = new RoleEntity();

        roleEntity.setRoleName(request.getRoleName());
        roleEntity.setPermissions(JSONUtil.parseArray(request.getPermissions()).toString());
        roleEntity.setDesc(request.getDesc());

        return roleDao.insertRole(roleEntity);
    }

    /**
     * 根据 id 查询角色
     * @param id         角色 id
     * @return           返回角色信息
     */
    @Override
    public RoleDetailVO getRoleById(Integer id) {
        // 获取该角色的所有权限
        // 获取角色信息
        RoleEntity roleEntity = roleDao.getRoleById(id);

        RoleDetailVO response = new RoleDetailVO();

        // 复制属性值到目标对象，但忽略 role 字段
        BeanUtil.copyProperties(roleEntity, response);

        return response;
    }

    /**
     * 修改角色信息
     * @param request    修改角色请求参数
     * @return           返回修改的行数
     */
    @Override
    @Transactional
    public int updateRole(RoleRequest request) {
        RoleEntity roleEntity = new RoleEntity();

        roleEntity.setId(request.getId());
        roleEntity.setRoleName(request.getRoleName());
        roleEntity.setDesc(request.getDesc());
        roleEntity.setPermissions(JSONUtil.parseArray(request.getPermissions()).toString());

        return roleDao.updateRole(roleEntity);
    }

    /**
     * 根据角色 id 查询拥有该角色的用户
     * @param roleId     角色 id
     * @return           返回拥有该角色的用户 id 列表
     */
    @Override
    public List<Integer> getUserIdByRoleId(Integer roleId) {
        return roleDao.getUserIdByRoleId(roleId);
    }

    /**
     * 删除角色
     * @param ids    角色 id 数组
     * @return       返回删除的行数
     */
    @Override
    @Transactional
    public int deleteRole(Integer[] ids) {
        if (roleDao.selectCanDelRole(ids)){
            return roleDao.deleteRole(ids);
        } else {
            throw new HisException("无法删除");
        }
    }
}
