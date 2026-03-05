package com.leehuang.his.api.db.dao;

import com.leehuang.his.api.db.pojo.CheckupResultEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultVO;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.CheckupProgressDTO;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CheckupResultDao {

    private final MongoTemplate mongoTemplate;

    /**
     * 插入体检结果快照
     * @param uuid          体检单唯一编号
     * @param name          体检人姓名
     * @param checkup       本次体检的体检项
     * @return
     */
    public String insert(String uuid, String name, List<CheckupVO> checkup) {
        CheckupResultEntity entity = new CheckupResultEntity();

        entity.setUuid(uuid);
        entity.setName(name);
        entity.setCheckinDate(LocalDateTime.now());

        entity.setCheckup(checkup);
        entity.setPlace(new ArrayList<>() {});
        entity.setResult(new ArrayList<>() {});
        //添加新记录
        entity = mongoTemplate.insert(entity);

        return entity.get_id();
    }

    /**
     * 根据预约的 uuid 查询体检结果
     * @param uuid
     * @return
     */
    public CheckupResultEntity searchResultByUuid(String uuid) {
        Criteria criteria = Criteria.where("uuid").is(uuid);
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query, CheckupResultEntity.class);
    }

    /**
     * 更新体检结果到 mongodb 中
     * @param uuid                   体检编号（唯一）
     * @param placeCheckupResultVO   某科室的体检结果
     */
    public void addCheckupResult(String uuid, PlaceCheckupResultVO placeCheckupResultVO) {

        // 1. 构造查询条件：查询 uuid 匹配 且 result.place 匹配的文档
        Query replaceQuery = new Query(
                Criteria.where("uuid").is(uuid)
                        .and("result.placeId").is(placeCheckupResultVO.getPlaceId())
        );

        // 2. 构造更新语句：将 result 数组中，第一个满足 replaceQuery 中条件的元素整体替换为 placeCheckupResultVO（因为 placeId 不会重复，查询出来的结果只有一个，所以直接替换就是更新体检结果）
        // $result.$ 中的 $（官方名称：Positional Operator） 表示 result 数组中的第一个满足 replaceQuery 中条件的元素
        Update replaceUpdate = new Update().set("result.$", placeCheckupResultVO);

        // 3. mongoTemplate.updateFirst 通过 replaceQuery 获取指定文档，replaceUpdate 更新指定数据，CheckupResultEntity.class 指定更新文档，执行原子更新
        UpdateResult replaceResult = mongoTemplate.updateFirst(
                replaceQuery,
                replaceUpdate,
                CheckupResultEntity.class
        );

        // 4. replaceResult 中的 getMatchedCount 表示匹配到的文档数量，无论更新数据是否与原数据相同，都表示找到了可以更新的文档，只要找到就能更新
        // getModifiedCount 表示更新操作实际修改的文档数量，如果更新数据与原数据相同，则修改数量为 0
        // getMatchedCount() > 0 直接 return，表示找到可以修改的文档，在上面执行了 updateFirst 更新后，就不用执行后面的追加操作
        if (replaceResult.getMatchedCount() > 0) {
            return; // 直接结束，避免后续逻辑执行
        }

        // 5. 如果没有替换成功，则该科室不存在，需要追加科室并添加体检结果
        // 构造查询仅按 uuid 查询的条件，不需要按照 uuid + result.place 来查询
        Query appendQuery = new Query(Criteria.where("uuid").is(uuid));

        // 6. 构造更新语句：向 place 列表中追加科室（addToSet 防止重复），并向 result 列表中追加体检结果
        // addToSet 是添加去重的简单对象，按值相等去重（存在则不添加，不存在则添加）；push 无条件追加，不检查是否重复，对象结构复杂时不能用 set
        Update appendUpdate = new Update()
                .addToSet("place", placeCheckupResultVO.getPlace())
                .push("result", placeCheckupResultVO);

        // 7. 执行追加操作
        UpdateResult appendResult = mongoTemplate.updateFirst(
                appendQuery,
                appendUpdate,
                CheckupResultEntity.class
        );

        // 如果 matchedCount == 0，说明 uuid 根本不存在
        if (appendResult.getMatchedCount() == 0) {
            throw new HisException("体检数据不存在");
        }
    }

    /**
     * 返回体检进度数据
     * @param uuid
     * @return
     */
    public CheckupProgressDTO getCheckupProgress(String uuid) {

        // 1. 根据 uuid 查询体检结果，只查询必要字段 name, checkup 和 result，避免整文档加载
        Query query = new Query(Criteria.where("uuid").is(uuid));
        query.fields().include("name").include("checkup").include("result");

        // 2. 执行查询，转换为 CheckupResultEntity
        CheckupResultEntity entity = mongoTemplate.findOne(query, CheckupResultEntity.class);

        if (entity == null) {
            throw new HisException("体检数据不存在");
        }

        if (entity.getCheckup() == null) {
            throw new HisException("没有找到体检项目，请咨询前台服务查询");
        }

        // 3. 从 checkup 中提取所有应完成的科室
        Set<String> allPlaces = entity.getCheckup().stream()
                .map(CheckupVO::getPlace).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. 从 result 获取中已完成的科室，若 entity.getResult() 为空则直接返回 null
        Map<String, String> finishedPlaceMap = Optional.ofNullable(entity.getResult())
                .orElse(Collections.emptyList())                    // 如果为null，返回空列表.stream()
                .stream()
                .collect(Collectors.toMap(e -> e.getPlaceId().toString(), PlaceCheckupResultVO::getPlace));

        // 5. 构造返回对象
        CheckupProgressDTO dto = new CheckupProgressDTO();
        dto.setName(entity.getName());
        dto.setAllPlaces(allPlaces);
        dto.setFinishedPlaces(finishedPlaceMap);

        dto.setAllFinished(finishedPlaceMap.values().containsAll(allPlaces));

        return dto;
    }
}
