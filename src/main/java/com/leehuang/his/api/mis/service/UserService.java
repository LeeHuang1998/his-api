package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.UserEntity;
import com.leehuang.his.api.mis.dto.user.request.InsertUserRequest;
import com.leehuang.his.api.mis.dto.user.request.UpdateUserRequest;
import com.leehuang.his.api.mis.dto.user.vo.UserPageVO;
import com.leehuang.his.api.mis.dto.user.vo.UserDetailVO;

import java.util.Map;


public interface UserService {

    // 用户登录
    UserEntity login(Map<String, Object> paramMap);

    // 修改用户密码
    Integer updatePassword(Map<String, Object> paramMap);

    // 获取用户分页数据
    PageUtils<UserPageVO> searchByPage(Map<String, Object> paramMap);

    // 检查用户名是否存在
    Integer checkUsername(String username);

    // 添加用户
    Integer insertUser(InsertUserRequest request);

    // 根据用户 id 查询用户
    UserDetailVO getUserById(Integer id);

    // 更新用户信息
    Integer updateUser(UpdateUserRequest request);

    // 根据 id 删除用户
    Integer deleteUserByIds(Integer[] ids);

    // 根据 id 更新用户在职状态
    Integer dismissUserById(Integer id);

    // 获取医生个人信息
    UserPageVO searchDoctorById(Integer userId);
}
