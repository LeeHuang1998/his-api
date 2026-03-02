package com.leehuang.his.api.db.pojo;

import com.leehuang.his.api.mis.dto.goods.vo.CheckupItemVo;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Document(collection  = "goods_snapshot")
public class GoodsSnapshotEntity implements Serializable {

    // @Id：声明该字段为 文档主键 _id，由 MongoDB 自动生成 UUID 主键值
    @Id
    private String _id;

    // @Indexed：告知框架在集合上为该字段创建普通单字段索引
    @Indexed
    private Integer goodsId;

    private String code;

    private String title;

    private String description;

    private List<CheckupItemVo> checkup1;

    private List<CheckupItemVo> checkup2;

    private List<CheckupItemVo> checkup3;

    private List<CheckupItemVo> checkup4;

    private String image;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private String type;

    private String[] tag;

    private String ruleName;

    private String rule;

    private List<CheckupVO> checkup;

    private String md5;
}
