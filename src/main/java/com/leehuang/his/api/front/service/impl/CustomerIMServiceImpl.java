package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.config.properties.TencentIMProperties;
import com.leehuang.his.api.db.dao.CustomerDao;
import com.leehuang.his.api.db.dao.CustomerImDao;
import com.leehuang.his.api.db.entity.CustomerEntity;
import com.leehuang.his.api.db.entity.CustomerImEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.customerIM.vo.CustomerIMAccountVO;
import com.leehuang.his.api.front.service.CustomerIMService;
import com.tencentyun.TLSSigAPIv2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

@Service("customerIMService")
@RequiredArgsConstructor
@Slf4j
public class CustomerIMServiceImpl implements CustomerIMService {

    private final CustomerDao customerDao;

    private final CustomerImDao customerImDao;

    private final TencentIMProperties imProperties;

    private final MinioProperties minioProperties;

    // 获取 tencentIm 配置
    private Long sdkAppId;
    private String managerId;
    private String secretKey;
    private String baseUrl;
    private String customerServiceId;

    // 在依赖注入完成后、Bean 初始化完毕时自动执行
    @PostConstruct
    public void init() {
        this.sdkAppId = imProperties.getSdkAppId();
        this.managerId = imProperties.getManagerId();
        this.secretKey = imProperties.getSecretKey();
        this.baseUrl = imProperties.getBaseUrl();
        this.customerServiceId = imProperties.getCustomerServiceId();
    }

    /**
     * 客户创建 im 账户
     * @param customerId
     * @return
     */
    @Override
    @Transactional
    public CustomerIMAccountVO createCustomerIMAccount(Integer customerId) {

        CustomerEntity entity = customerDao.searchCustomerBaseInfoById(customerId);
        String account = "customer_" + customerId;

        // 生成客户账户签名
        TLSSigAPIv2 tlsSigAPIv2 = new TLSSigAPIv2(sdkAppId, secretKey);
        String userSig = tlsSigAPIv2.genUserSig(account, 180 * 86400);

        // 创建前端展示 IM 账户
        CustomerIMAccountVO imAccountVO = new CustomerIMAccountVO();
        imAccountVO.setSdkAppId(sdkAppId);
        imAccountVO.setAccount(account);
        imAccountVO.setUserSig(userSig);

        // 生成管理员账户签名
        String managerUserSig = tlsSigAPIv2.genUserSig(imProperties.getManagerId(), 180 * 86400);

        // 1. 查询 客户IM 账户是否存在
        Boolean checkResult = this.checkAccountImportIM(account, managerUserSig, customerId);
        // 2. 若不存在则创建
        if (Boolean.FALSE.equals(checkResult)) {
            String createResult = this.createImAccountInIM(managerUserSig, entity);
            // 添加客服好友
            String addFriendResult = this.addFriend(managerUserSig, account, customerId);

            if (!"OK".equals(createResult) || !"OK".equals(addFriendResult)) {
                throw new HisException("客服系统异常");
            }
        }

        // 发送欢迎词
        this.sendWelcomeMessage(account);

        return imAccountVO;
    }

    /**
     * 查询自有账号是否已导入即时通信 IM
     * @param account       客户 IM 账号名
     * @param userSig       管理员签名
     * @param customerId    客户 id
     * @return
     */
    private Boolean checkAccountImportIM(String account, String userSig, Integer customerId) {
        // 1. 创建查询账号接口请求，用于查询自有账号是否已导入即时通信 IM，支持批量查询。
        String url = baseUrl + "v4/im_open_login_svc/account_check?sdkappid=" +
                sdkAppId + "&identifier=" + managerId + "&usersig=" + userSig +
                "&random=" + RandomUtil.randomInt(1, 99999999) + "&contenttype=json";

        // 接口请求体
        JSONObject json = new JSONObject();
        json.set("CheckItem", new ArrayList<>() {{
            add(new HashMap<>() {{
                put("UserID", account);
            }});
        }});

        // 2. 发送请求查询 客户IM 账户是否存在
        String response = HttpUtil.post(url, json.toString());

        // 3. 解析返回结果
        JSONObject entries = JSONUtil.parseObj(response);

        // 3.1 errorCode：0 表示成功，非 0 表示失败
        Integer errorCode = entries.getInt("ErrorCode");
        // 请求处理失败时的错误信息
        String errorInfo = entries.getStr("ErrorInfo");
        // 若有错误抛异常
        if (errorCode != 0) {
            log.error("查询客户IM账号失败：{}", errorInfo);
            throw new HisException("客服系统异常");
        }

        // 3.2 没有错误，返回账号检查结果对象数组，只查询了一个账号，直接取第一个元素即可
        JSONArray resultItem = (JSONArray) entries.get("ResultItem");
        JSONObject object = (JSONObject) resultItem.get(0);

        // AccountStatus：查询账号的导入状态，Imported 表示已导入，NotImported 表示未导入
        String accountStatus = object.getStr("AccountStatus");

        // 4. 判断是否存在 客户IM 账号，若存在则直接返回
        if ("Imported".equals(accountStatus)) {
            // 4.1 更新 IM 账号登陆时间到数据库

            int rows = customerImDao.update(null,
                    new LambdaUpdateWrapper<CustomerImEntity>()
                            .eq(CustomerImEntity::getCustomerId, customerId)
                            .set(CustomerImEntity::getLoginTime, LocalDateTime.now())
            );

            if (rows == 0) {
                log.error("无法更新客户IM账号登录时间，客户账号ID：{}", customerId);
                throw new HisException("客服系统异常");
            }

            return true;
        }

        // 4.3 若没有查到，则直接返回 false
        return false;
    }

    /**
     * 在 im 中创建账户
     * @return
     */
    private String createImAccountInIM(String userSig, CustomerEntity entity) {
        // 2.2 若不存在，则创建新账户
        String url = baseUrl + "v4/im_open_login_svc/account_import?sdkappid=" +
                sdkAppId + "&identifier=" + managerId + "&usersig=" +
                userSig + "&random=" + RandomUtil.randomInt(1, 99999999) +
                "&contenttype=json";

        String account = "customer_" + entity.getId();
        String nickname = "客户_" + entity.getName();

        JSONObject json = new JSONObject();
        json.set("UserID", account);
        json.set("Nick", nickname);
        if (entity.getPhoto() != null) {
            String photo = minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + entity.getPhoto();
            json.set("FaceUrl", photo);
        }

        // 发送请求创建 客户IM 账户
        String response = HttpUtil.post(url, json.toString());
        JSONObject entries = JSONUtil.parseObj(response);

        Integer errorCode = entries.getInt("ErrorCode");
        String errorInfo = entries.getStr("ErrorInfo");

        if (errorCode != 0) {
            log.error("创建客户 IM 账号失败：{}", errorInfo);
            throw new HisException("客服系统异常");
        }

        CustomerImEntity customerImEntity = new CustomerImEntity();
        customerImEntity.setCustomerId(entity.getId());
        customerImEntity.setLoginTime(LocalDateTime.now());
        int rows = customerImDao.insert(customerImEntity);

        if (rows == 0) {
            throw new HisException("客户 IM 数据插入系统异常");
        }

        return entries.getStr("ActionStatus");
    }

    /**
     * 给 客户IM 账号添加客服好友
     * @param userSig
     * @param account
     * @param customerId
     * @return
     */
    private String addFriend(String userSig, String account, Integer customerId) {
        // 1. 创建添加好友接口
        String url = baseUrl + "v4/sns/friend_add?sdkappid=" + sdkAppId +
                "&identifier=" + managerId + "&usersig=" + userSig +
                "&random=" + RandomUtil.randomInt(1, 99999999) +
                "&contenttype=json";

        // 请求体 From_Account：客户IM 账号，To_Account：客服IM 账号
        JSONObject json = new JSONObject();
        json.set("From_Account", account);
        json.set("AddFriendItem", new ArrayList<>() {{
            add(new HashMap<>() {{
                put("To_Account", customerServiceId);
                put("AddSource", "AddSource_Type_Web");
            }});
        }});

        // 2. 发送添加好友的请求
        String response = HttpUtil.post(url, json.toString());
        JSONObject entries = JSONUtil.parseObj(response);
        Integer errorCode = entries.getInt("ErrorCode");
        String errorInfo = entries.getStr("ErrorInfo");

        if (errorCode != 0) {
            log.error("添加客服 IM 好友失败:{}", errorInfo);
            throw new HisException("客服系统异常");
        }

        // 3. 批量加好友的结果对象数组，此处只添加了一个客服好友，只取第一个即可
        JSONArray resultItem = (JSONArray) entries.get("ResultItem");
        JSONObject object = (JSONObject) resultItem.get(0);

        // To_Account：请求添加的好友的 UserID
        // ResultCode：To_Account 的处理结果，0 表示成功，非 0 表示失败，非 0 取值的详细描述请参见
        int resultCode = object.getInt("ResultCode");
        // resultInfo：To_Account 的错误描述信息，成功时该字段为空
        String resultInfo = object.getStr("ResultInfo");
        if (resultCode != 0) {
            log.error("添加客服IM好友失败：{}", resultInfo);
            throw new HisException("客服系统异常");
        }

        // 4. 添加好友完成后，向 customerIm 表中保存客户 id，表示该客户有 im 账号
        CustomerImEntity existImAccount =
                customerImDao.selectOne(new LambdaQueryWrapper<CustomerImEntity>().eq(CustomerImEntity::getCustomerId, customerId));
        // 若存在则更新时间，若不存在则插入
        if (existImAccount == null) {
            CustomerImEntity newImAccount = new CustomerImEntity();
            newImAccount.setCustomerId(customerId);
            newImAccount.setLoginTime(LocalDateTime.now());
            customerImDao.insert(newImAccount);
        } else {
            existImAccount.setLoginTime(LocalDateTime.now());
            customerImDao.updateById(existImAccount);
        }
        return entries.getStr("ActionStatus");
    }


    /**
     * 发送欢迎词
     * @param account   客户 IM 账号
     */
    private void sendWelcomeMessage(String account) {

        //生成客服账号签名
        TLSSigAPIv2 tlsSigAPIv2 = new TLSSigAPIv2(sdkAppId, secretKey);
        String userSig = tlsSigAPIv2.genUserSig(customerServiceId, 180 * 86400);

        // 单发单聊消息接口
        String url = baseUrl + "v4/openim/sendmsg?sdkappid=" + sdkAppId +
                "&identifier=" + customerServiceId + "&usersig=" + userSig +
                "&random=" + RandomUtil.randomInt(1, 99999999) + "&contenttype=json";

        // 请求体
        JSONObject json = new JSONObject();
        // 消息不同步至发送方（欢迎词不需要同步），若不填写默认情况下会将消息存 From_Account 漫游
        json.set("SyncOtherMachine", 2);                                // 1：把消息同步到 From_Account 在线终端和漫游上，2：消息不同步至 From_Account，3：消息不同步至 To_Account
        json.set("To_Account", account);                                // 消息接收方 UserID
        json.set("MsgLifeTime", 120);                                   // 消息保存两分钟
        json.set("MsgRandom", RandomUtil.randomInt(1, 99999999));       // 消息随机数（32位无符号整数），用于消息去重（后台用于同一秒内的消息去重）。确保该字段是随机数
        json.set("MsgBody", new ArrayList<>() {{                        // 消息内容
            add(new HashMap<>() {{
                put("MsgType", "TIMTextElem");                          //文本消息
                put("MsgContent", new HashMap<>() {{
                    put("Text", "亲，您好，非常高兴为您服务，有什么可以为您效劳的呢?");
                }});
            }});
        }});

        // 发送请求，请求响应
        String response = HttpUtil.post(url, json.toString());

        JSONObject entries = JSONUtil.parseObj(response);
        // 获取错误码，若为 0 表示成功
        int errorCode = entries.getInt("ErrorCode");
        // 错误信息
        String errorInfo = entries.getStr("ErrorInfo");
        if (errorCode != 0) {
            log.error("发送欢迎词失败：{}", errorInfo);
            throw new HisException("客服系统异常");
        }
    }
}
