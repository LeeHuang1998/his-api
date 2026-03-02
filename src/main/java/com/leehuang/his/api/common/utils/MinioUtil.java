package com.leehuang.his.api.common.utils;

import com.leehuang.his.api.exception.BizCodeEnum;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.exception.FileException;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MinioUtil {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient minioClient;                // minio 服务器连接


    // 创建 minio 连接，@PostConstruct 注解确保在对象创建并完成依赖注入后，执行某些初始化逻辑
    @PostConstruct
    public void init(){
        // 创建 minio 连接
        this.minioClient = new MinioClient.Builder()
                .endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    /**
     * 上传图片的封装方法
     * @param path      上传文件的路径
     * @param file      上传的文件
     */
    public void uploadImage(String path, MultipartFile file){
        try {
            // PutObjectArgs 是 MinIO SDK 中的一个参数容器类（或称为值对象）。用于封装所有向 MinIO 服务端发送 putObject 请求所需的配置参数（即用于封装 putObject 方法的参数）。
            // .bucket：存储桶名称    object：存储桶中文件夹的名称
            // .stream：上传的文件流，第一个参数为上传文件的输入流，第二个参数为数据流的总大小，未知则为 -1，第三个参数为限制上传文件的大小，最大为 5MB（该限制的最小值为 5M，无法比 5MB 更小）
            // .contentType：上传文件的类型
            this.minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(file.getInputStream(), -1, 5 * 1024 * 1024)
                    .contentType("image/jpeg")
                    .build());
            log.debug("向：{} 路径保存了 image 文件", path);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            throw new FileException(6001, "上传图片失败");
        }
    }

    /**
     * 上传 base64 图片
     * @param path                  上传路径
     * @param base64Image           上传图片的 base64 编码（去掉前缀后的纯 base64 字符串）
     * @param contentType          上传图片的 MIME 类型
     */
    public void uploadBase64Image(String path, String base64Image, String contentType) {
        // 1. 参数基础验证
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (base64Image == null || base64Image.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64图片数据不能为空");
        }
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("contentType 必须是 image/* 类型");
        }

        // 2. Base64 解码
        byte[] imageBytes;
        try {
            // 将 Base64 字符串转为原始字节数组（即图片二进制数据）
            imageBytes = Base64.getDecoder().decode(base64Image);
        } catch (IllegalArgumentException e) {
            throw new HisException("无效的 Base64 编码", e);
        }

        // 3. 校验大小（防止 OOM）
        final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
        if (imageBytes.length > MAX_SIZE) {
            throw new HisException("图片大小不能超过 5MB");
        }

        // 4. 上传到 MinIO，在 try 括号中声明的资源，会在 try 块结束时（无论正常结束还是抛出异常）自动调用 .close() 方法
        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(in, imageBytes.length, MAX_SIZE)
                    .contentType(contentType)
                    .build());
            log.debug("成功上传图片到 MinIO: {}", path);
        } catch (Exception e) {
            log.error("MinIO 上传失败，path={}", path, e);
            throw new HisException("保存图片失败", e);
        }
    }

    /**
     * 根据 Base64 图片字符串解析出 MIME 类型和文件扩展名
     * @param base64Image               包含或不包含 data:image/...;base64, 前缀的 Base64 图片字符串
     * @return                          数组，第一个元素为纯 Base64 字符串，第二个元素为 MIME 类型，第三个元素为文件扩展名
     */
    public String[] getMimeTypeAndExtension(String base64Image) {
        String pureBase64;
        String contentType = "image/jpeg"; // 默认值
        String extension = ".jpg"; // 默认扩展名

        // 判断是否为标准 Data URL 格式（HTML5 Canvas、FileReader 常见输出格式）
        if (base64Image.startsWith("data:image/")) {
            // 使用正则提取 MIME 和 Base64，parts 第一个元素为头信息（包含 MIME），第二个元素为 base64 字符串
            String[] parts = base64Image.split(",");

            if (parts.length != 2) {
                throw new HisException("无效的 base64 图片格式");
            }

            // 头信息（data:image/png;base64）
            String header = parts[0];
            // 纯 base64 字符串
            pureBase64 = parts[1];

            // 获取 contentType 和文件扩展类型（Content-Type 是 HTTP 头部的一个属性，其值是一个 MIME 类型）
            if (header.contains("jpeg") || header.contains("jpg")) {
                contentType = "image/jpeg";
                extension = ".jpg";
            } else if (header.contains("png")) {
                contentType = "image/png";
                extension = ".png";
            } else if (header.contains("gif")) {
                contentType = "image/gif";
                extension = ".gif";
            } else if (header.contains("webp")) {
                contentType = "image/webp";
                extension = ".webp";
            } else {
                contentType = "image/octet-stream"; // 未知类型
                extension = ".bin"; // 未知类型的默认扩展名
            }
        } else {
            // 无前缀，视为纯 Base64，默认 jpeg
            pureBase64 = base64Image;
        }
        return new String[]{pureBase64, contentType, extension};
    }

    /**
     * 批量删除图片的封装方法
     * @param objectList      删除文件的路径
     */
    public void removeImages(List<DeleteObject> objectList){
        try {
            // removeObjects 返回的是 “懒执行” 的 Iterable<Result<DeleteError>>
            // 必须把迭代器完整遍历一遍，MinIO 才会真正向服务端发出 DeleteObjects 请求；否则调用立即返回，一个文件也不会删。
            // 调用 removeObjects 并获取结果迭代器
            Iterable<Result<DeleteError>> results = this.minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(bucket)
                    .objects(objectList)
                    .build());

            // ！！！关键：遍历结果，否则删除可能不会真正生效:cite[3]
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get(); // 这里如果单个删除有错误，会抛出异常或者返回 DeleteError 对象
                if (error != null) {
                    // 记录或处理单个文件的删除错误
                    log.error("删除文件失败: {} - {}", error.objectName(), error.message());
//                    throw new HisException("删除文件失败: " + error.objectName());
                }
            }
            log.debug("批量删除文件请求已完成：{}", objectList);
        } catch (Exception e) {
            log.error("删除文件过程发生异常", e); // 这里会捕获 result.get() 抛出的异常或其他异常
            throw new HisException("删除文件失败");
        }
    }

    /**
     * 上传 Excel 的封装方法
     * @param path       上传文件的路径
     * @param file       上传的文件，最大不超过 20MB
     */
    public void uploadExcel(String path, MultipartFile file){
        try {
            // 设置 mime（即 contentType）
            String mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            // 上传 excel 文件
            this.minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(file.getInputStream(), -1, 20 * 1024 *1024)
                    .contentType(mime).build()
            );
            log.debug("向：{} 路径保存了 excel 文件", path);
        } catch (Exception e) {
            log.error("上传 excel 文件失败", e);
            throw new FileException(6001, "上传 excel 文件失败");
        }
    }

    /**
     * 下载 Excel 的封装方法
     * @param path          下载文件的路径
     * @return              文件输入流，用于写入文件
     */
    public InputStream downloadExcel(String path){
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build();
            return this.minioClient.getObject(args);
        } catch (Exception e) {
            log.error("excel 文件下载失败：{}", path, e);
            throw new FileException(BizCodeEnum.DOWNLOAD_EXCEPTION.getCode(), "路径为：" + path + "的 excel 文件下载失败");
        }
    }

    /**
     * 生成预签名 URL
     * @param objectPath         文件路径
     * @param expiryMinutes      URL 过期时间，单位为分钟
     * @return
     */
    public String generatePresignedUrl(String objectPath, int expiryMinutes) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectPath)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build();

            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("生成预签名 URL 失败：{}", objectPath, e);
            throw new FileException(BizCodeEnum.DOWNLOAD_EXCEPTION.getCode(), "生成预签名URL失败");
        }
    }
}
