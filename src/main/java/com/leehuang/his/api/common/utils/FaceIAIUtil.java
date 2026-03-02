package com.leehuang.his.api.common.utils;

import com.leehuang.his.api.exception.HisException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 通过编写代码调用人脸识别服务 API
 */
@Component
@ConfigurationProperties(prefix = "tencent.cloud")
@Slf4j
@Data
public class FaceIAIUtil {

    private String secretId;
    private String secretKey;

    private Face face = new Face();

    @Data
    public static class Face {
        private String region;
        private String groupId;
    }

    /**
     * 使用腾讯云主账号，创建子账号，使用子账号的 secretId 和 secretKey。保证权限范围最小
     * 创建子账号后，给子账号授权（添加策略），使用 JSON 格式，添加策略如下："iai:CompareFace", "iai:DetectLiveFaceAccurate", "iai:CreatePerson", "iai:GetPersonBaseInfo"
     */
    private IaiClient iaiClient;

    // TODO 配置文件中的属性明文
    @PostConstruct
    public void init() {
        // 密钥信息从环境变量读取，需要提前在环境变量中设置 TENCENTCLOUD_SECRET_ID 和 TENCENTCLOUD_SECRET_KEY
        // 使用环境变量方式可以避免密钥硬编码在代码中，提高安全性
        // 生产环境建议使用更安全的密钥管理方案，如密钥管理系统(KMS)、容器密钥注入等
        // 请参见：https://cloud.tencent.com/document/product/1278/85305
        // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
        Credential cred = new Credential(secretId, secretKey);
        // 使用临时密钥示例
        // Credential cred = new Credential("SecretId", "SecretKey", "Token");
        // 实例化一个http选项，可选的，没有特殊需求可以跳过
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("iai.tencentcloudapi.com");
        httpProfile.setConnTimeout(10000);
        httpProfile.setReadTimeout(10000);

        // 实例化一个client选项，可选的，没有特殊需求可以跳过
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        this.iaiClient = new IaiClient(cred, face.getRegion(), clientProfile);
        log.info("IAI Client initialized for region: {}", face.getRegion());
    }

    /**
     * 人脸对比+活体验证，接口：人脸对比（CompareFaceRequest）+ 静态活体检测（DetectLiveFaceAccurateRequest）
     * 人脸识别流程：人脸对比（身份证图片和拍摄图片静态对比） -> 拍摄图片静态活体校验 -> 验证通过后，判断人员库中是否有该人员 -> 若有，则直接返回验证通过
     *                                                                                                  -> 若没有，则将该人员添加到人员库中后，再返回验证通过
     * @param name    姓名
     * @param pid     身份证号码
     * @param gender  性别
     * @param photo_1 身份证照片
     * @param photo_2 摄像头照片
     * @return
     */
    public boolean verifyFaceModel(String name, String pid, Long gender, String photo_1, String photo_2) {

        // 1. 创建 执行人脸比对 请求
        CompareFaceRequest compareFaceRequest = new CompareFaceRequest();
        compareFaceRequest.setImageA(photo_1);
        compareFaceRequest.setImageB(photo_2);
        compareFaceRequest.setQualityControl(4L);           // 图片质量要求（4 为最高），眼鼻嘴不能遮挡，各个维度均为最好或最多在某一维度上存在轻微问题；
        CompareFaceResponse compareFaceResponse;
        try {
            compareFaceResponse = this.iaiClient.CompareFace(compareFaceRequest);
        } catch (TencentCloudSDKException e) {
            if (e.getErrorCode().equals("FailedOperation.FaceQualityNotQualified")) {
                log.error("人脸质量不合格", e);
                throw new HisException("人脸质量不合格");
            }
            log.error("人脸比对失败", e);
            throw new HisException("人脸比对失败");
        }
        // 人脸比对的分数
        if (compareFaceResponse.getScore() >= 85) {
            log.info("人脸比对通过，分数：{}", compareFaceResponse.getScore());
            // 执行静态活体验证
            DetectLiveFaceAccurateRequest detectLiveFaceRequest = new DetectLiveFaceAccurateRequest();
            detectLiveFaceRequest.setImage(photo_2);
            DetectLiveFaceAccurateResponse detectLiveFaceAccurateResponse;
            try {
                detectLiveFaceAccurateResponse = this.iaiClient.DetectLiveFaceAccurate(detectLiveFaceRequest);
                // 静态活体检测打分，取值范围 [0,100]，根据活体分数对应的阈值区间来判断是否为翻拍，目前阈值可分为[5,10,40,70,90]，其中推荐阈值为40
                if ( detectLiveFaceAccurateResponse.getScore() < 40) {
                    return false;
                }
            } catch (TencentCloudSDKException e) {
                if (e.getErrorCode().equals("FailedOperation.FaceQualityNotQualified")) {
                    log.error("静态活体识别图片解码失败");
                    throw new HisException("静态活体识别图片解码失败");
                }
                log.error("静态活体识别失败", e);
                throw new HisException("静态活体识别失败");
            }
        } else {
            // 不是同一人，直接返回 false
            return false;
        }

        // 判断人员库是否有该体检人
        String personBaseInfo = this.getPersonFaceId(pid);
        // 若没有该体检人且通过人脸对比和静态活体检测
        if (personBaseInfo == null) {
            // 人员库中没有该人员，创建人员
            String person = this.createPerson(name, pid, gender, photo_1);
            if (!StringUtils.isBlank(person)) {
                log.info("创建人员成功，人员 faceID：{}", person);
            }
        }

        return true;
    }

    /**
     * 根据条件查询人员记录，接口：GetPersonBaseInfo
     * @param pid           身份证号，人员库中的人员id
     * @return              返回体检人 faceId
     */
    public String getPersonFaceId(String pid) {
        // 创建请求查询人员库中是否有该体检人
        GetPersonBaseInfoRequest getPersonBaseInfoRequest = new GetPersonBaseInfoRequest();
        getPersonBaseInfoRequest.setPersonId(pid);

        try {
            // 查询结果
            GetPersonBaseInfoResponse getPersonBaseInfoResponse = this.iaiClient.GetPersonBaseInfo(getPersonBaseInfoRequest);
            // 若 faceIds 存在，则返回第一个元素，若不存在则返回 null
            return getPersonBaseInfoResponse.getFaceIds() == null ? null : getPersonBaseInfoResponse.getFaceIds()[0];
        } catch (TencentCloudSDKException e) {
            if (e.getErrorCode().equals("InvalidParameterValue.PersonIdNotExist")) {
                log.info("查询人员库失败，人员 ID 不存在", e);
                return null;
            } else {
                log.error("查询人员库失败", e);
                throw new HisException("查询人员库失败");
            }
        }
    }

    /**
     * 往人员库中添加人员
     * @param personName    人员名称。[1，60] 个字符，可修改，可重复。
     * @param pid           人员ID。单个腾讯云账号下不可修改，不可重复。身份证为值
     * @param gender        性别：0 代表未填写，1 代表男性，2 代表女性
     * @param image         人员图片，图片 base64 数据
     * @return              返回创建人员的 FaceId
     */
    public String createPerson(String personName, String pid, Long gender, String image) {
        CreatePersonRequest request = new CreatePersonRequest();

        // 设置人员信息
        request.setPersonName(personName);
        request.setPersonId(pid);                   // 人员 id，不可修改，不可重复
        request.setGender(gender);
        request.setImage(image);
        request.setQualityControl(4L);              // 图片质量要求（4 为最高），眼鼻嘴不能遮挡，各个维度均为最好或最多在某一维度上存在轻微问题；
        request.setUniquePersonControl(4L);         // 判断 Image 中图片包含的人脸，是否在人员库中已有疑似的同一人（4 为 十万一误识别率，最高识别判断要求）
        request.setGroupId(face.getGroupId());      // 人员库 id

        try {
            CreatePersonResponse createPersonResponse = this.iaiClient.CreatePerson(request);
            return createPersonResponse.getFaceId();
        } catch (TencentCloudSDKException e) {
            if (e.getErrorCode().equals("InvalidParameterValue.PersonIdAlreadyExist")) {
                log.warn("创建人员失败，人员 [{}，{}] 已存在，跳过创建", personName, pid);
                return null;
            } else if (e.getErrorCode().equals("InvalidParameterValue.GroupIdNotExist")) {
                log.error("创建人员失败，该人员库不存在", e);
                throw new HisException("创建人员失败，该人员库不存在", e);
            } else {
                log.error("创建人员失败", e);
                throw new HisException("创建人员失败", e);
            }
        }
    }
}
