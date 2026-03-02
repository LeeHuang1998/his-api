package com.leehuang.his.api.db.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leehuang.his.api.db.pojo.GoodsSnapshotEntity;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GoodsSnapshotDao {

    private final MongoTemplate mongoTemplate;

    // 使用Jackson的ObjectMapper来解析JSON字符串
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据 MD5 查询快照
     * @param md5
     * @return
     */
    public String findSnapshotIdByMd5(String md5) {
        Criteria criteria = Criteria.where("md5").is(md5);
        Query query = new Query(criteria);
        query.skip(0);
        query.limit(1);
        GoodsSnapshotEntity entity = mongoTemplate.findOne(query, GoodsSnapshotEntity.class, "goods_snapshot");
        return entity != null ? entity.get_id() : null;
    }

    /**
     * 插入快照
     * @param entity    订单快照
     * @return          插入成功后返回 MongoDB 中数据的 _id（类似于主键 id）
     */
    public String insert(GoodsSnapshotEntity entity) {
        return mongoTemplate.save(entity).get_id();
    }

    /**
     * 根据 _id 查询快照
     * @param _id
     * @return
     */
    public GoodsSnapshotEntity getGoodsSnapshotById(String _id) {
        return mongoTemplate.findById(_id, GoodsSnapshotEntity.class);
    }

    /**
     * 聚合查询（checkup 大数据量时），根据 ID 和性别筛选检查项目
     * @param id 文档ID
     * @param sex 体检人性别 ("男" 或 "女")
     * @return 筛选后的检查项目列表
     */
    public List<CheckupVO> searchCheckupWithAggregation(String id, String sex) {
        // 1. 构建正确的聚合管道
        Aggregation aggregation = Aggregation.newAggregation(
                // 1.1 根据 _id 匹配到目标文档（使用 ObjectId）
                Aggregation.match(Criteria.where("_id").is(new ObjectId(id))),
                // 1.2 展开数组
                Aggregation.unwind("$checkup"),
                // 1.3 在展开的子文档中筛选 sex 字段为 "无" 或指定性别的检查项目
                Aggregation.match(Criteria.where("checkup.sex").in("无", sex)),
                // 1.4 将子文档提升为根，并映射到实体类
                Aggregation.replaceRoot("$checkup")
        );

        // 2. 执行聚合，直接映射到 CheckupVO 类型
        AggregationResults<CheckupVO> results = mongoTemplate.aggregate(
                aggregation,
                "goods_snapshot",
                CheckupVO.class // 直接输出为目标类型
        );

        return results.getMappedResults();
    }
}
