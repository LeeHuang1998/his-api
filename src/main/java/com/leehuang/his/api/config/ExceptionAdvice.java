package com.leehuang.his.api.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.felord.payment.PayException;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.exception.BizCodeEnum;
import com.leehuang.his.api.exception.FileException;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.exception.PermissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Objects;

/**
 * 全局异常处理类
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    // 权限异常
    @ExceptionHandler(PermissionException.class)
    public R handlePermissionException(PermissionException e) {
        log.error("权限错误：", e);
        return R.error(BizCodeEnum.PERMISSION_EXCEPTION.getCode(), BizCodeEnum.PERMISSION_EXCEPTION.getMsg());
    }

    // 文件异常
    @ExceptionHandler(FileException.class)
    public R handleFileException(FileException e) {
        if (e.getCode() == BizCodeEnum.UPLOAD_EXCEPTION.getCode()) {
            log.error("文件上传错误：", e);
        } else if (e.getCode() == BizCodeEnum.DOWNLOAD_EXCEPTION.getCode()) {
            log.error("文件下载错误：", e);
        }
        return R.error(e.getCode(), e.getMessage());
    }

    // 捕获异常，并返回 500 状态码（此处异常都为服务器内部处理异常）
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)                          // 捕获并处理 Exception 及其子类异常
    public R exceptionHandler(Exception e) {

        // 若前端 Ajax 请求未提交数据或提交的数据错误
        if (e instanceof HttpMessageNotReadableException) {
            HttpMessageNotReadableException exception = (HttpMessageNotReadableException) e;
            log.error("请求未提交数据或参数有误", exception);      // 详细错误信息输出到日志中
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "请求未提交数据或参数有误");
        }

        // 上传文件时数据错误，如上传文件不存在、上传文件有错误、上传数据错误、上传文件时还上传其他的数据等等
        else if (e instanceof MissingServletRequestPartException) {
            MissingServletRequestPartException exception = (MissingServletRequestPartException) e;
            log.error("请求提交的数据错误", exception);          // 详细错误信息输出到日志中
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "请求提交的数据错误");
        }

        // 请求方式错误
        else if (e instanceof HttpRequestMethodNotSupportedException) {
            HttpRequestMethodNotSupportedException exception = (HttpRequestMethodNotSupportedException) e;
            log.error("HTTP 请求方式错误", exception);          // 详细错误信息输出到日志中
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "HTTP 请求方式错误");
        }

        // Ajax 请求提交的数据后端验证未通过
        else if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException exception = (MethodArgumentNotValidException) e;

            // 防止 getFieldError() 为空导致空指针
            String defaultMessage = Objects.requireNonNull(exception.getBindingResult().getFieldError()).getDefaultMessage();

            log.error("参数校验未通过：{}", defaultMessage, exception);
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), defaultMessage);
        }

        // web 方法参数数据类型转换异常，如需要 String[] 数组类型参数，但上传的时 String
        else if (e instanceof BindException) {
            BindException exception = (BindException) e;

            // 防止 getFieldError() 为空导致空指针
            String defaultMessage = exception.getFieldError() != null
                    ? exception.getFieldError().getDefaultMessage()
                    : "参数绑定失败";

            log.error("数据类型转换异常：{}", defaultMessage, exception);      // 获取是数据类型转换异常的字段
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "数据类型转换异常：" + defaultMessage);     // 获取未通过验证的字段
        }

        // 业务处理异常
        else if (e instanceof HisException) {
            HisException exception = (HisException) e;
            log.error("业务执行异常", e);                      // 详细错误信息输出到日志中
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMsg());
        }

        // 微信支付异常
        else if (e instanceof PayException) {
            PayException exception = (PayException) e;
            log.error("微信支付异常", e);                      // 详细错误信息输出到日志中
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "微信支付异常：" + exception.getMessage());
        }

        // 其他异常
        else {
            log.error("其他异常", e);                      // 详细错误信息输出到日志中
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统异常，请稍后重试");
        }
    }

    // 登录异常，返回 401 状态码
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotLoginException.class)                          // sa-token 中的异常，若前端传入的令牌错误或没有传入令牌则触发该异常
    public R notLoginExceptionHandler(NotLoginException e) {
        log.error("登录异常：", e);
        return R.error(HttpStatus.UNAUTHORIZED.value(), "令牌错误：" + e.getMessage());
    }
}