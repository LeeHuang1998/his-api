package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.leehuang.his.api.common.utils.MinioUtil;
import com.leehuang.his.api.config.properties.AliyunProperties;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.db.dao.CustomerDao;
import com.leehuang.his.api.db.entity.CustomerEntity;
import com.leehuang.his.api.db.entity.CustomerThirdPartyEntity;
import com.leehuang.his.api.exception.BizCodeEnum;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.customer.CustomerBindDTO;
import com.leehuang.his.api.front.dto.customer.request.*;
import com.leehuang.his.api.front.dto.customer.request.validator.LoginRequestValidator;
import com.leehuang.his.api.front.dto.customer.vo.CustomerVO;
import com.leehuang.his.api.front.dto.customer.vo.LoginVO;
import com.leehuang.his.api.front.dto.customer.vo.SmsCodeVO;
import com.leehuang.his.api.front.service.CustomerService;
import com.leehuang.his.api.front.service.CustomerThirdPartyService;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("customerService")
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final LoginRequestValidator requestValidator;

    // Service 中注入的名称与 RedisTemplateConfig 中的方法名一致
    private final RedisTemplate<String, String> redisTemplateDb0;

    private final MinioUtil minioUtil;

    private final CustomerDao customerDao;

    private final CustomerThirdPartyService thirdPartyService;

    private final MinioProperties minioProperties;

    private final Client dypnsClient;

    private final AliyunProperties aliyunProperties;

    private final static String CODE_PREFIX = "sms:code:";

    private final static String LOCK_PREFIX = "sms:lock:";

    /**
     * 发送短信验证码
     * @param smsCodeRequest    手机号
     */
    @Override
    public SmsCodeVO sendSmsCode(SmsCodeRequest smsCodeRequest) {
        SmsCodeVO smsCodeVO = new SmsCodeVO();
        String phoneNum = smsCodeRequest.getPhoneNum();

        // 1. 防刷：检查 60 秒内是否重复发送
        String lockKey = LOCK_PREFIX + phoneNum;
        Boolean isLocked = redisTemplateDb0.opsForValue().setIfAbsent(lockKey, "1", 60, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isLocked)) {
            smsCodeVO.setResult(false);
            smsCodeVO.setMessage(Objects.requireNonNull(redisTemplateDb0.getExpire(lockKey, TimeUnit.SECONDS)).toString());
        }

        // 2. 生成 6 位随机验证码
        String code = RandomUtil.randomNumbers(6);
        // 短信模板
        String templateParam = "{\"code\":\"" + code + "\", \"min\":" + aliyunProperties.getSms().getExpireMinutes() + "}";

        try {
            // 3. 调用阿里云 SDK 发送短信
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phoneNum)
                    .setSignName(aliyunProperties.getSms().getSignName())
                    .setTemplateCode(aliyunProperties.getSms().getTemplateCode())
                    .setTemplateParam(templateParam);

            SendSmsVerifyCodeResponse response = dypnsClient.sendSmsVerifyCode(request);

            // response.getBody().getCode() 为 "OK" 表示成功
            if ("OK".equals(response.getBody().getCode())) {
                smsCodeVO.setResult(true);
                smsCodeVO.setMessage("短信验证码已发送");

                // 4. 将验证码存入 Redis，设置过期时间
                String redisKey = CODE_PREFIX + phoneNum;
                redisTemplateDb0.opsForValue().set(redisKey, code, aliyunProperties.getSms().getExpireMinutes(), TimeUnit.MINUTES);

                log.info("短信发送成功：phone={}, code={}, requestId={}", phoneNum, code, response.getBody().getRequestId());
            } else {
                // 失败：记录错误码和描述
                String errorCode = response.getBody().getCode();
                String errorMessage = response.getBody().getMessage();
                log.error("短信发送失败，错误码：{}，错误信息：{}", errorCode, errorMessage);
                throw new HisException("短信发送失败，错误码：" + errorCode);
            }
        } catch (TeaException e) {
            // TeaException 主要用于捕获 网络、HTTP 状态码、IO 或配置错误，是阿里云 SDK 处理请求异常的核心类
            log.error("阿里云短信发送异常：phone={}, message={}", phoneNum, e.getMessage());
            throw new HisException("短信发送失败，请检查手机号或稍后重试");
        } catch (Exception e) {
            log.error("系统异常", e);
            throw new HisException("系统繁忙，请稍后再试");
        }
        return smsCodeVO;
    }

    /**
     * 登录
     * @param loginRequest
     * @return
     */
    @Override
    public LoginVO login(CustomerRequest loginRequest) {
        // 获取登录类型
        String loginType = loginRequest.getType();

        if ("sms".equals(loginType)) {
            requestValidator.validateSmsLogin(loginRequest);
            return loginBySmsCode(loginRequest);
        } else if ("password".equals(loginType)) {
            requestValidator.validatePasswordLogin(loginRequest);
            return loginByPassword(loginRequest);
        } else {
            throw new HisException("登录类型错误");
        }
    }

    /**
     * 注册
     * @param request
     * @return
     */
    @Override
    @Transactional
    public LoginVO register(RegisterRequest request) {
        LoginVO loginVO = new LoginVO();
        String phoneNum = request.getPhoneNum();

        // 验证码校验
        validateCode(phoneNum, request.getSmsCode());

        // 注册用户
        CustomerEntity entity = customerDao.searchCustomerByTel(phoneNum);
        if (entity != null) {
            throw new HisException(BizCodeEnum.USER_EXIST.getCode(), BizCodeEnum.USER_EXIST.getMsg());
        }

        // 插入到数据库中
        CustomerEntity customer = new CustomerEntity();

        customer.setUsername(phoneNum);
        customer.setPassword(BCrypt.hashpw(request.getPassword()));
        customer.setTel(phoneNum);

        // 插入新纪录
        customerDao.insertCustomer(customer);

        Integer id = customer.getId();

        // 生成 token
        StpCustomerUtil.login(id, "PC");
        String tokenValue = StpCustomerUtil.getTokenValue();

        loginVO.setUsername(phoneNum);
        loginVO.setToken(tokenValue);

        return loginVO;
    }

    /**
     * 第三方登录并注册
     * @param request
     * @return
     */
    @Override
    @Transactional
    public LoginVO thirdPartyRegister(ThirdPartyRequest request) {
        LoginVO loginVO = new LoginVO();

        String phoneNum = request.getPhoneNum();

        // 验证码校验
        validateCode(phoneNum, request.getSmsCode());

        // 判断该用户是否已存在
        CustomerEntity entity = customerDao.searchCustomerByTel(phoneNum);
        if (entity != null) {
            throw new HisException(BizCodeEnum.USER_EXIST.getCode(), BizCodeEnum.USER_EXIST.getMsg());
        }

        // 不存在则插入新用户到数据库中
        CustomerEntity customer = new CustomerEntity();

        customer.setUsername(phoneNum);
        customer.setPassword(BCrypt.hashpw(request.getPassword()));
        customer.setTel(phoneNum);
        if (request.getEmail() != null) {
            customer.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            customer.setPhoto(request.getAvatar());
        }

        int customerId = customerDao.insertCustomer(customer);

        CustomerThirdPartyEntity thirdPartyEntity = new CustomerThirdPartyEntity();
        thirdPartyEntity.setCustomerId(customerId);
        thirdPartyEntity.setNickname(request.getNickname());
        thirdPartyEntity.setOpenId(request.getOpenId());
        thirdPartyEntity.setAvatar(request.getAvatar());
        thirdPartyEntity.setPlatform(request.getPlatform());

        // 保存第三方平台信息到第三方用户表中
        thirdPartyService.insertThirdPartyCustomer(thirdPartyEntity);

        // 生成 token
        StpCustomerUtil.login(customerId, "PC");
        String tokenValue = StpCustomerUtil.getTokenValue();

        loginVO.setUsername(phoneNum);
        loginVO.setToken(tokenValue);
        return loginVO;
    }

    /**
     * 第三方登录并绑定
     * @param request
     * @return
     */
    @Override
    @Transactional
    public LoginVO thirdPartyBind(ThirdPartyRequest request) {
        LoginVO loginVO = new LoginVO();

        // 查询当前账号是否已被绑定
        CustomerBindDTO bindDTO = thirdPartyService.searchByTelAndPlatform(request.getPhoneNum(), request.getPlatform());

        if (bindDTO == null) {
            // 用户不存在
            throw new HisException(BizCodeEnum.USER_NOT_EXIST.getCode(), BizCodeEnum.USER_NOT_EXIST.getMsg());
        } else if (bindDTO.getCustomerId() != null && bindDTO.getThirdPartyId() != null) {
            // 用户已被其他账号绑定
            throw new HisException(BizCodeEnum.USER_BIND_EXIST.getCode(), BizCodeEnum.USER_BIND_EXIST.getMsg());
        } else if (bindDTO.getCustomerId() == null && bindDTO.getThirdPartyId() != null) {
            throw new HisException("绑定错误，请联系管理员");
        } else {
            // 当前账号不存在，绑定该用户
            CustomerThirdPartyEntity thirdPartyEntity = new CustomerThirdPartyEntity();

            thirdPartyEntity.setCustomerId(bindDTO.getCustomerId());
            thirdPartyEntity.setNickname(request.getNickname());
            thirdPartyEntity.setOpenId(request.getOpenId());
            thirdPartyEntity.setAvatar(request.getAvatar());
            thirdPartyEntity.setPlatform(request.getPlatform());

            thirdPartyService.insertThirdPartyCustomer(thirdPartyEntity);

            // 登录
            StpCustomerUtil.login(bindDTO.getCustomerId(), "PC");
            String tokenValue = StpCustomerUtil.getTokenValue();
            loginVO.setToken(tokenValue);
            loginVO.setUsername(request.getPhoneNum());
        }

        return loginVO;
    }

    /**
     * 更新头像
     * @param file              头像图片
     * @param oldPath           用户原头像
     */
    @Override
    @Transactional
    public void updateAvatar(MultipartFile file, String oldPath) {
        // 获取当前登录用户
        int customerId = StpCustomerUtil.getLoginIdAsInt();

        // 若 oldPath 不为空字符串，则需要删除原图片
        if (oldPath.startsWith(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/")) {
            List<String> newList = new ArrayList<>(List.of(oldPath));

            List<DeleteObject> deleteObjList
                    = newList.stream().map(DeleteObject::new).collect(Collectors.toList());
            minioUtil.removeImages(deleteObjList);
        }

        // 获取原文件名
        String fileName = file.getOriginalFilename();

        assert fileName != null;
        // 获取扩展名（含点）
        String extension  = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        // 设置上传路径和文件名
        String path = "front/customer/photo/customer_" + customerId + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
        // 上传文件
        minioUtil.uploadImage(path, file);

        // 更新数据库数据
        customerDao.updatePhoto(customerId, path);
    }

    /**
     * 获取用户信息
     * @return
     */
    @Override
    public CustomerVO getCustomerInfo() {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        CustomerVO customerInfo = customerDao.getCustomerInfo(customerId);
        customerInfo.setPhoto(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + customerInfo.getPhoto());
        return customerInfo;
    }

    /**
     * 更新用户信息
     * @param request
     */
    @Override
    @Transactional
    public void updateCustomerInfo(CustomerInfoRequest request) {
        // 查询更改的用户名是否已存在
        CustomerEntity entity = customerDao.searchCustomerByUsernmae(request.getUsername());
        int customerId = StpCustomerUtil.getLoginIdAsInt();

        if (entity != null && entity.getId() != customerId) {
            throw new HisException(BizCodeEnum.USER_EXIST.getCode(), BizCodeEnum.USER_EXIST.getMsg());
        }

        customerDao.updateCustomerInfo(request,customerId);
    }

    /**
     * 修改手机号或密码
     * @param request
     */
    @Override
    @Transactional
    public void updateTelOrPassword(CustomerRequest request) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        String updateType = request.getType();

        // 根据不同的类型进行修改
        if ("sms".equals(updateType)) {
            requestValidator.validateSmsLogin(request);
            // 验证码校验
            validateCode(request.getIdentity(), request.getCredential());
            // 判断新手机号是否存在
            CustomerEntity entity = customerDao.searchCustomerByTel(request.getIdentity());
            if (entity != null && entity.getId() != customerId) {
                throw new HisException(BizCodeEnum.USER_EXIST.getCode(), "该手机号已绑定其他账号");
            }
            // 校验通过后，修改手机号
            customerDao.updateTel(request.getIdentity(), customerId);

        } else if ("password".equals(updateType)) {
            // 对比原密码
            CustomerEntity entity = customerDao.searchCustomerById(customerId);
            if (entity != null && !BCrypt.checkpw(request.getIdentity(), entity.getPassword())) {
                // 原密码错误
                throw new HisException(BizCodeEnum.PASSWORD_ERROR.getCode(), "原密码错误");
            }
            // 修改密码
            customerDao.updatePassword(BCrypt.hashpw(request.getCredential()), customerId);
        } else {
            throw new HisException("修改类型错误");
        }
    }


    /**
     * 短信验证码登录
     * @param loginRequest
     * @return
     */
    private LoginVO loginBySmsCode(CustomerRequest loginRequest) {
        LoginVO loginVO = new LoginVO();

        String identity = loginRequest.getIdentity();

        // 验证码校验
        validateCode(identity, loginRequest.getCredential());

        // 判断当前用户是否存在与数据库中
        CustomerEntity entity = customerDao.searchCustomerByTel(identity);
        if (entity == null) {
            throw new HisException(BizCodeEnum.USER_NOT_EXIST.getCode(), BizCodeEnum.USER_NOT_EXIST.getMsg());
        }

        loginVO.setUsername(entity.getUsername());

        // 生成 token
        StpCustomerUtil.login(entity.getId(), "PC");
        String token = StpCustomerUtil.getTokenValue();
        loginVO.setToken(token);

        return loginVO;
    }

    /**
     * 密码登录
     * @param loginRequest
     * @return
     */
    private LoginVO loginByPassword(CustomerRequest loginRequest) {
        LoginVO loginVO = new LoginVO();

        String identity = loginRequest.getIdentity();
        String credential = loginRequest.getCredential();

        // 查找是否有该用户
        CustomerEntity entity = customerDao.searchCustomerByUsernmae(identity);
        if (entity == null) {
            throw new HisException(BizCodeEnum.USER_NOT_EXIST.getCode(), BizCodeEnum.USER_NOT_EXIST.getMsg());
        }

        // 比对密码
        if (!BCrypt.checkpw(credential, entity.getPassword())) {
            throw new HisException(BizCodeEnum.PASSWORD_ERROR.getCode(), BizCodeEnum.PASSWORD_ERROR.getMsg());
        }

        // 生成 token
        StpCustomerUtil.login(entity.getId(), "PC");
        String token = StpCustomerUtil.getTokenValue();

        loginVO.setToken(token);
        loginVO.setUsername(entity.getUsername());

        return loginVO;
    }

    /**
     * 验证码校验
     * @param identity      手机号
     * @param smsCode       验证码
     */
    private void validateCode(String identity, String smsCode){

        String key = CODE_PREFIX + identity;

        // 判断当前短信验证码是否有效
        if (Boolean.FALSE.equals(redisTemplateDb0.hasKey(key))){
            throw new HisException(BizCodeEnum.SMS_CODE_EXPIRE.getCode(), BizCodeEnum.SMS_CODE_EXPIRE.getMsg());
        }

        // 判断当前短信验证码是否正确
        String storedCode = redisTemplateDb0.opsForValue().get(key);
        assert storedCode != null;
        if (!storedCode.equals(smsCode)){
            throw new HisException(BizCodeEnum.SMS_CODE_ERROR.getCode(), BizCodeEnum.SMS_CODE_ERROR.getMsg());
        }

//        // 删除当前短信验证码
//        redisTemplateDb0.delete(key);
//        key = "sms_code_refresh_" + identity;
//        redisTemplateDb0.delete(key);
    }

}
