package com.leehuang.his.api.mis.dto.goods.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * imagesUrl 注解校验
 * 实现了 ConstraintValidator<注解类型, 被校验的数据类型> 接口，告诉框架：当字段上标了 @ImagesPattern 且字段类型是 String[] 时，由我来校验。
 */
public class ImagesPatternValidator implements ConstraintValidator<ImagesPattern, String[]> {

    // 正则表达式对象，用来逐一匹配数组里的每个字符串
    private Pattern pattern;

    // 框架在第一次遇到带有 @ImagesPattern 注解的字段时，会调用一次 initialize，把注解里的元数据传进来。
    @Override
    public void initialize(ImagesPattern constraintAnnotation) {
        // 获取注解里的正则表达式，编译成 Pattern 对象，避免每次 isValid 都重新编译正则
        pattern = Pattern.compile(constraintAnnotation.regexp());
    }

    // 框架在每次校验时，都会调用一次 isValid，把要校验的值传进来
    @Override
    public boolean isValid(String[] strings, ConstraintValidatorContext constraintValidatorContext) {
        // 若 strings 为空，则通过校验，让 @NotEmpty 去报空数组，不重复报错
        if (strings == null) {
            return true;
        }
        // 逐一匹配数组里的每个字符串
        for (String v : strings) {
            // 只要出现 null 或者不符合正则的字符串，立即返回 false，触发校验失败
            if (v == null || !pattern.matcher(v).matches()) {
                return false;
            }
        }
        return true;
    }
}
