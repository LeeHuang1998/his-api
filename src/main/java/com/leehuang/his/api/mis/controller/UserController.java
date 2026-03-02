package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.db.entity.UserEntity;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.common.request.LoginRequest;
import com.leehuang.his.api.mis.dto.user.vo.UserDetailVO;
import com.leehuang.his.api.mis.dto.user.vo.UserPageVO;
import com.leehuang.his.api.mis.dto.user.request.*;
import com.leehuang.his.api.mis.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mis/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     * @param loginRequest 前端传递的 username 和 password
     * @return        登录结果
     */
    @PostMapping("/login")
    public R login(@RequestBody @Valid LoginRequest loginRequest){
        // 将 loginRequest 转换为 Map 对象，因为 loginRequest 中含有后端验证表达式，该对象仅用于 web 层，不适合传给 service 层
        Map<String, Object> paramMap = BeanUtil.beanToMap(loginRequest);

        // 获取登录用户信息
        UserEntity userEntity = userService.login(paramMap);

        if (userEntity != null) {
            Integer status = userEntity.getStatus();
            // 判断是否在职，登录时同端互斥
            if ( status == 1 ){
                // 获取用户 id
                Integer userId = userEntity.getId();
                // 先登出再登录，防止其他同端没有退出，参数为 登录的用户 和 登录客户端（必须写明客户端，否则生成的令牌不一致）
                StpUtil.logout(userId, "web");
                StpUtil.login(userId,"web");
                // 生成 token
                String token = StpUtil.getTokenValueByLoginId(userId, "web");
                // 生成 token 后，获取当前用户的权限列表
                List<String> permissions = StpUtil.getPermissionList(userId);
                // 将 token 和 permission 返回到前端
                return R.OK().put("result", true).put("token", token).put("permissions", permissions).put("name", userEntity.getName());
            } else if (status == 2){
                return R.OK().put("result", false).put("msg", "离职用户无法登录");
            } else {
                return R.OK().put("result", false).put("msg", "用户被禁用，请联系管理员");
            }
        }

        // 登录失败，返回 false
        return R.OK().put("result", false).put("msg", "用户名或密码错误");
    }

    /**
     * 用户登出，@saCHeckLogin 注解用于校验用户是否登录，未登录则无法访问该方法
     */
    @GetMapping("/logout")
    @SaCheckLogin
    public R logout(){
        // 从令牌中解密获取 userId
        int userId = StpUtil.getLoginIdAsInt();
        StpUtil.logout(userId,"web");
        return R.OK();
    }

    /**
     * 修改密码
     * @param request  前端传递的密码数据
     * @return         返回修改行数，成功为 1，否则为 0
     */
    @PostMapping("/updatePassword")
    @SaCheckLogin
    public R updatePassword(@RequestBody @Valid UpdatePasswordRequest request){
        // 获取 userId
        int userId = StpUtil.getLoginIdAsInt();
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", userId);
        paramMap.put("password", request.getPassword());
        paramMap.put("newPassword", request.getNewPassword());

        Integer rows = userService.updatePassword(paramMap);

        return R.OK().put("rows", rows);
    }

    /**
     * 分页查询用户列表，必须拥有权限：ROOT，USER:SELECT
     * @param request 分页查询前端传递的查询条件
     * @return   查询的数据，存放在 resp.pageData 中
     */
    @PostMapping("/getUserListByPage")
    @SaCheckPermission(value = {"ROOT", "USER:SELECT"}, mode = SaMode.OR)
    public R getUserByPage(@RequestBody @Valid UserPageRequest request){

        // 获取当前页码和每页记录数
        Integer page = request.getPage();
        Integer length = request.getLength();

        // 每页开始的数据 id
        int start = (page - 1) * length;

        Map<String, Object> paramMap = BeanUtil.beanToMap(request);

        paramMap.put("start", start);
        paramMap.put("length", length);

        PageUtils<UserPageVO> pageUtils = userService.searchByPage(paramMap);

        return R.OK().put("pageData", pageUtils);
    }

    /**
     * 检查用户名是否重复
     * @param request
     * @return
     */
    @PostMapping("/checkUsername")
    @SaCheckPermission(value = {"ROOT", "USER:INSERT"}, mode = SaMode.OR)
    public R checkUsername(@RequestBody CheckUsernameRequest request){
        Integer exists = userService.checkUsername(request.getUsername());

        if (exists == 1){
            return R.OK().put("exists", exists).put("msg", "用户名已存在");
        }
        return R.OK().put("exists", exists).put("msg", "用户名可用");
    }

    /**
     * 新增用户，必须拥有权限：ROOT，USER:INSERT
     * @param request     前端传递的新增用户数据
     * @return            返回新增行数，成功为 1，否则为 0
     */
    @PostMapping("/insertUser")
    @SaCheckPermission(value = {"ROOT", "USER:INSERT"}, mode = SaMode.OR)
    public R addUser(@RequestBody @Valid InsertUserRequest request){
        Integer rows = userService.insertUser(request);
        return R.OK().put("rows", rows);
    }


    /**
     * 根据 id 查询用户，必须拥有权限：ROOT，USER:SELECT
     * @param request       用户 id
     * @return              查询到的用户对象
     */
    @PostMapping("/getUserById")
    @SaCheckPermission(value = {"ROOT", "USER:SELECT"}, mode = SaMode.OR)
    public R getUserById(@RequestBody @Valid IdRequest request){
        UserDetailVO userById = userService.getUserById(request.getId());
        return R.OK().put("updateUserInfo", userById);
    }

    /**
     * 修改用户信息
     * @param request   前端传递的修改后的用户数据
     * @return          返回修改行数，成功为 1，否则为 0
     */
    @PostMapping("/updateUser")
    @SaCheckPermission(value = {"ROOT", "USER:UPDATE"}, mode = SaMode.OR)
    public R updateUser(@RequestBody @Valid UpdateUserRequest request){
        Integer rows = userService.updateUser(request);
        // 如果修改成功，则退出被修改用户的账号，包括 web 端、app 端、小程序端等
        if (rows == 1){
            StpUtil.logout(request.getId());
        }
        return R.OK().put("rows", rows);
    }

    /**
     * 根据 id 删除用户，必须拥有权限：ROOT，USER:DELETE
     * @param request 前端传递的用户 id 数组
     * @return        返回删除行数，成功为 1，否则为 0
     */
    @PostMapping("/deleteUserByIds")
    @SaCheckPermission(value = {"ROOT", "USER:DELETE"}, mode = SaMode.OR)
    public R deleteUserByIds(@RequestBody @Valid IdsRequest request){
        Integer[] ids = request.getIds();

        // 判断删除的账户是否为自己，若是则无法删除
        if(Arrays.stream(ids).anyMatch(id -> id.equals(StpUtil.getLoginIdAsInt()))){
            return R.error("无法删除自己的账户");
        }

        // 删除用户
        Integer rows = userService.deleteUserByIds(ids);

        // 如果删除成功，则退出被删除用户的账号，包括 web 端、app 端、小程序端等
        if (rows > 0) {
            for (Integer id : ids) {
                StpUtil.logout(id);
            }
        }
        return R.OK().put("rows", rows);
    }

    /**
     * 员工离职
     * @param request
     * @return
     */
    @PostMapping("/dismissUserById")
    @SaCheckPermission(value = {"ROOT", "USER:UPDATE"}, mode = SaMode.OR)
    public R dismissUserById(@RequestBody @Valid IdRequest request){
        Integer rows = userService.dismissUserById(request.getId());

        // 将离职用户退出登录
        if (rows == 1) {
            StpUtil.logout(request.getId());
        } else {
            return R.OK().put("rows", rows).put("msg", "离职操作失败");
        }
        return R.OK().put("rows", rows);
    }

    /**
     * 获取医生信息
     * @return
     */
    @GetMapping("/searchDoctorById")
    @SaCheckLogin
    public R searchDoctorById() {
        Integer userId = StpUtil.getLoginIdAsInt();
        UserPageVO userPageVO = userService.searchDoctorById(userId);
        return R.OK().put("doctorVO", userPageVO);
    }
}
