package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.UserEntity;
import com.leehuang.his.api.db.dao.UserDao;
import com.leehuang.his.api.mis.dto.user.request.InsertUserRequest;
import com.leehuang.his.api.mis.dto.user.request.UpdateUserRequest;
import com.leehuang.his.api.mis.dto.user.vo.UserPageVO;
import com.leehuang.his.api.mis.dto.user.vo.UserDetailVO;
import com.leehuang.his.api.mis.service.UserService;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Resource
    private UserDao userDao;

    /**
     * 密码加盐
     * @param username 用户名
     * @param password 密码
     * @return 加密后的密码
     */
    private String salting(String username, String password){
        MD5 md5 = MD5.create();

        String temp = md5.digestHex(username);
        String tempStart = temp.substring(0,6);
        String tempEnd = temp.substring(temp.length()-3);

        return md5.digestHex(tempStart + password + tempEnd).toUpperCase();
    }

    /**
     * 用户登录
     * @param paramMap   用户登录参数 map，包含 username 和 password
     * @return           返回查找到的用户记录条数，若存在则返回
     */
        @Override
        public UserEntity login(Map<String, Object> paramMap) {
            String username = MapUtils.getString(paramMap, "username");
            String password = MapUtils.getString(paramMap, "password");

            password = salting(username, password);

            return userDao.login(username, password);
        }

    /**
     * 修改密码
     * @param paramMap  修改密码数据
     * @return  修改成功则返回 1，否则返回 0
     */
    @Override
    public Integer updatePassword(Map<String, Object> paramMap) {
        // 获取数据
        int userId = MapUtils.getIntValue(paramMap, "userId");
        String password = MapUtils.getString(paramMap, "password");
        String newPassword = MapUtils.getString(paramMap, "newPassword");

        if (password != null && newPassword != null) {
            // 获取用户名
            String username = userDao.queryUsernameById(userId);
            // 无需与原密码进行对比，若原密码错误则无法修改数据
            password = salting(username, password);

            newPassword = salting(username, newPassword);
            // 成功修改数据返回 1，否则返回 0
            return userDao.updatePassword(userId, password, newPassword);
        }

        return 0;
    }

    /**
     * 获取用户分页记录
     * @param paramMap  前端传入的查询条件，包括 name，sex，角色，部门 和 status
     * @return          返回分页结果
     */
    @Override
    public PageUtils<UserPageVO> searchByPage(Map<String, Object> paramMap) {
        List<UserPageVO> pageList = new ArrayList<>();
        long userCount = userDao.getUserCount(paramMap);
        if (userCount > 0) {
            pageList = userDao.getUserListByPage(paramMap);
        }
        int page = MapUtils.getIntValue(paramMap, "page");
        int length = MapUtils.getIntValue(paramMap, "length");

        return new PageUtils<>(userCount, page, length, pageList);
    }

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 存在则返回 true，否则返回 false
     */
    @Override
    public Integer checkUsername(String username) {
        return userDao.checkUsername(username);
    }

    /**
     * 添加用户
     * @param request     用户数据传输对象
     * @return            添加成功则返回 1，否则返回 0
     */
    @Override
    @Transactional
    public Integer insertUser(InsertUserRequest request) {
        // 将数据传输对象转换为实体对象
        UserEntity userEntity = BeanUtil.toBean(request, UserEntity.class);

        // 设置默认数据
        userEntity.setStatus(1);
        userEntity.setPassword(salting(request.getUsername(), request.getPassword()));

        // 设置角色，接收前端参数时，使用 Integer[] 接收，需要转换为 JSON 数组后再转换为字符串，才能插入到数据库中
        userEntity.setRole(JSONUtil.parseArray(request.getRole()).toString());

        // 返回成功修改行数
        return userDao.insertUser(userEntity);
    }

    /**
     * 根据 id 获取用户信息
     * @param id  用户 id
     * @return 查询到的用户对象
     */
    @Override
    public UserDetailVO getUserById(Integer id) {
        UserEntity userById = userDao.getUserById(id);

        UserDetailVO response = new UserDetailVO();
        // 复制属性值到目标对象，但忽略 role 字段
        BeanUtil.copyProperties(userById, response, "role");

        // 从数据库中查出的 role 为 String，需要转换为 Integer[]，\\s 表示匹配任意空白字符（空格、\t、\n 等），* 表示匹配 0 次或多次
        // 1. 去除首尾的方括号和空格
        String roleString = userById.getRole().replaceAll("[\\[\\]\\s]", "");
        // 2. 按逗号分割并转换为 Integer[]
        Integer[] roleArr = Arrays.stream(roleString.split(",")).map(String::trim).map(Integer::parseInt).toArray(Integer[]::new);
        // 3. 设置到目标对象中
        response.setRole(roleArr);

        return response;
    }

    /**
     * 修改用户信息
     * @param request   前端传递的修改后的信息
     * @return          修改成功则返回 1，否则返回 0
     */
    @Override
    @Transactional
    public Integer updateUser(UpdateUserRequest request) {
        UserEntity userEntity = new UserEntity();

        // 复制除 role 外的属性到 userEntity 中
        BeanUtil.copyProperties(request, userEntity, "role");
        // 设置角色，接收前端参数时，使用 Integer[] 接收，需要转换为 JSON 数组后再转换为字符串，才能插入到数据库中
        userEntity.setRole(JSONUtil.parseArray(request.getRole()).toString());
        // 返回成功修改行数
        return userDao.updateUser(userEntity);
    }

    /**
     * 根据 id 删除用户
     * @param ids 用户 id 数组
     * @return 删除成功则返回 1，否则返回 0
     */
    @Override
    @Transactional
    public Integer deleteUserByIds(Integer[] ids) {
        return userDao.deleteUserByIds(ids);
    }

    /**
     * 根据用户 id 修改用户在职状态
     * @param id 用户 id
     * @return 修改成功则返回 1，否则返回 0
     */
    @Override
    public Integer dismissUserById(Integer id) {
        // 管理员无法离职
        return userDao.dismissUserById(id);
    }

    /**
     * 获取医生个人信息
     * @param userId
     * @return
     */
    @Override
    public UserPageVO searchDoctorById(Integer userId) {
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<UserEntity> eq = new LambdaQueryWrapper<UserEntity>()
                .select(UserEntity::getName, UserEntity::getSex, UserEntity::getTel)
                .eq(UserEntity::getId, userId);

        UserEntity userEntity = userDao.selectOne(eq);
        UserPageVO userPageVO = new UserPageVO();
        BeanUtil.copyProperties(userEntity, userPageVO);

        return userPageVO;
    }
}
