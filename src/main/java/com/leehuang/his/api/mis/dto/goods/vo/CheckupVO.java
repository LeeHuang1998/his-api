package com.leehuang.his.api.mis.dto.goods.vo;

import lombok.*;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CheckupVO {

    private String sex;

    private String code;                // 模板编码

    private String item;                // 具体的检查内容，即 name 的具体检查项目

    private String name;                // 检查项目

    private String type;                // 检查结果采集方式

    private String place;               // 检查科室

    private String value;               // 检查结果

    private String template;            // 输出模板

    // 静态工厂方法，专门用于展示体检引导单项目的数据
    public static CheckupVO of(String name, String place) {
        CheckupVO vo = new CheckupVO();
        vo.setName(name);
        vo.setPlace(place);
        return vo;
    }

    /**
    * 只要 name 和 place 相同，则视为同一个对象
    **/
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckupVO)) return false;
        CheckupVO that = (CheckupVO) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(place, that.place);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, place);
    }
}
