package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.UserEntity;
import com.leehuang.his.api.mis.dto.user.vo.UserPageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author 16pro
* @description 针对表【tb_user(用户表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.UserEntity
*/
public interface UserDao extends BaseMapper<UserEntity> {
    // 根据用户 id 查询用户权限
    Set<String> getUserPermissions(@Param("userId") int userId);

    // 用户登录，根据传入的参数在数据库中查找数据，成功查找则返回数据记录
    UserEntity login(@Param("username") String username, @Param("password") String password);

    // 查找用户名
    String queryUsernameById(@Param("userId") int userId);

    // 修改用户密码
    Integer updatePassword(@Param("userId") int userId, @Param("password") String password, @Param("newPassword") String newPassword);

    // 获取用户数据列表
    List<UserPageVO> getUserListByPage(Map<String,Object> paramMap);

    // 获取用户记录总数
    long getUserCount(Map<String,Object> paramMap);

    // 检查用户名是否存在
    Integer checkUsername(@Param("username") String username);

    // 添加用户
    Integer insertUser(UserEntity userEntity);

    // 根据用户 id 查询 用户信息
    UserEntity getUserById(@Param("userId") Integer userId);

    // 修改用户信息
    Integer updateUser(UserEntity userEntity);

    // 根据 id 删除用户
    Integer deleteUserByIds(@Param("ids") Integer[] ids);

    // 根据 id 修改用户在职状态
    Integer dismissUserById(@Param("id") Integer id);

}




