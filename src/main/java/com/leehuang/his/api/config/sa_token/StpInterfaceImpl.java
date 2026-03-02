package com.leehuang.his.api.config.sa_token;

import cn.dev33.satoken.stp.StpInterface;
import com.leehuang.his.api.db.dao.UserDao;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserDao userDao;

    /**
     *  返回传入用户的所有权限
     * @param loginId       用户 id，即 userId
     * @param loginType     用户类型
     * @return  用户拥有的权限集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 将 loginId 转换为 Integer
        int userId = Integer.parseInt(loginId.toString());
        // 查询用户拥有的权限集合
        Set<String> userPermissions = userDao.getUserPermissions(userId);
        // 返回权限集合
        return new ArrayList<>(userPermissions);
    }

    /**
     * 返回传入用户的所有角色（暂时不实现，不建议通过角色判断权限，因为权限类型是固定的，一般不会删减，但是角色不是固定的，通过角色鉴权会导致角色被删除时需要修改代码）
     * @param loginId       用户 id，即 userId
     * @param loginType     用户类型
     * @return  用户拥有的角色集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }
}
