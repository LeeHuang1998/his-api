package com.leehuang.his.api.mis.dto.goods.validation;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImagesPatternValidator.class)
public @interface ImagesPattern {

    String message() default "存在非法字符或长度超限";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String regexp();   // 必填：正则表达式
}
