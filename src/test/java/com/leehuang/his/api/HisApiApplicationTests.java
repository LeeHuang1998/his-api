package com.leehuang.his.api;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leehuang.his.api.common.constants.redis.FlowRegulationConstants;
import com.leehuang.his.api.db.dao.*;
import com.leehuang.his.api.db.entity.FlowRegulationEntity;
import com.leehuang.his.api.db.entity.GoodsEntity;
import com.leehuang.his.api.db.pojo.CheckupResultEntity;
import com.leehuang.his.api.db.pojo.GoodsSnapshotEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.customer.vo.CustomerVO;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;
import com.leehuang.his.api.front.service.CustomerService;
import com.leehuang.his.api.front.service.impl.CustomerServiceImpl;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultVO;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.CheckupProgressDTO;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import com.leehuang.his.api.mis.service.MisGoodsService;
import com.leehuang.his.api.mis.service.UserService;
import com.mongodb.client.result.UpdateResult;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
class HisApiApplicationTests {

    @Resource
    private MisGoodsService goodsService;

    @Resource
    private CustomerServiceImpl customerServiceimpl;

    @Resource
    private CustomerDao customerDao;

    @Resource
    private GoodsDao goodsDao;

    @Resource
    private GoodsSnapshotDao goodsSnapshotDao;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CheckupResultDao checkupResultDao;

    @Resource
    private MongoTemplate mongoTemplate;
    @Autowired
    private FlowRegulationDao flowRegulationDao;

    @Test
    void contextLoads() {

    }

    @Test
    void textGoodsSnapshot(){
    }

    @Test
    void testGetPartGoods() {

//        String username = "zhangsan";
//
//        String password = "123456";
//
//        String salting = customerServiceimpl.salting(username, password);
//
//        System.out.println(salting);

    }

    @Test
    void testCustomerDao(){
        CustomerVO customerInfo = customerDao.getCustomerInfo(1);
        System.out.println(customerInfo);
    }

    @Test
    void testSnapshot() throws JsonProcessingException {

        String _id = "694b933dabf6c43594f510fd";

        GoodsSnapshotEntity goodsSnapshotById = goodsSnapshotDao.getGoodsSnapshotById(_id);
        System.out.println("总的体检项目总数" + goodsSnapshotById.getCheckup().size());

        List<CheckupVO> filterCheckupList = goodsSnapshotById.getCheckup().stream()
                .filter(checkup -> "无".equals(checkup.getSex()) || "男".equals(checkup.getSex())).collect(Collectors.toList());
        System.out.println("性别筛选后的项目总数" + filterCheckupList.size());
        List<CheckupVO> nonCheckup = goodsSnapshotById.getCheckup().stream()
                .filter(checkup -> "女".equals(checkup.getSex())).collect(Collectors.toList());
        System.out.println("性别不符合的项目总数" + nonCheckup.size());

        CheckupResultEntity checkupResultEntity = checkupResultDao.searchResultByUuid("165A1AB1725F4D25AFEE5B91AD5A04F3");
        System.out.println("checkup_Result 中的体检项目总数" + checkupResultEntity.getCheckup().size());
        System.out.println("checkup_Result 中有结果的项目总数" + checkupResultEntity.getResult().size());

        // 在筛选性别后的基础上根据科室获取该科室的体检项目数量
        List<CheckupVO> placeCheckupList = filterCheckupList.stream().filter(checkup -> "外科".equals(checkup.getPlace())).collect(Collectors.toList());
        System.out.println("根据科室获取该科室的体检项目数量" + placeCheckupList.size());

        System.out.println("--------------------------------------------------");


        // 科室
        HashMap<String, Integer> placeMap = new HashMap<>();
        for (CheckupVO filterCheckup : filterCheckupList) {
            String place = filterCheckup.getPlace();

            // 判断 map 中是否有当前可是
            if (!placeMap.containsKey(place)) {
                placeMap.put(place, 1);
            } else {
                Integer i = placeMap.get(place);
                Integer placeTotal = i + 1;
                placeMap.put(place, placeTotal);
            }
        }

        int sum = 0;
        for (String key : placeMap.keySet()) {
            sum += placeMap.get(key);
            System.out.println("科室：" + key + " 当前科室体检项目总数：" + placeMap.get(key) + " 体检项目总数总和：" + sum);
        }

        System.out.println("---------------------------------------------------------------------------------");

        int resultSum = 0;
        for (PlaceCheckupResultVO checkupResult : checkupResultEntity.getResult()) {
            int size = checkupResult.getCheckupItems().size();
            resultSum += size;
            System.out.println("当前有结果的体检项目总数：" + resultSum);
        }

        System.out.println("---------------------------------------------------------------------------------");
        System.out.println("体检项目总数：" + checkupResultEntity.getResult().size());
        int allResult = 0;
//        checkupResultEntity.getResult().stream().map(resultEntity -> resultEntity.getCheckupItems().size()).forEach();
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRecommendNextPlace() {
        CheckupProgressDTO progressDTO = checkupResultDao.getCheckupProgress("165A1AB1725F4D25AFEE5B91AD5A04F3");

        // 判断是否所有体检都已完成，若所有体检都完成，不再推荐下一个科室，返回 null
        if (progressDTO.isAllFinished()) {
            System.out.println("所有体检都已完成");
        }

        // 2. 获取所有已完成体检项目的科室
        Map<String, String> finishedPlaces = progressDTO.getFinishedPlaces();

        Set<ZSetOperations.TypedTuple<String>> candidates =
                stringRedisTemplate.opsForZSet().rangeWithScores(FlowRegulationConstants.FLOW_PLACE_RANK, 0, -1);

        // 若为空则直接返回
        if (candidates == null || candidates.isEmpty()) {
            throw new HisException("没有候选科室");
        }

        candidates.forEach(tuple -> System.out.println("原来的候选科室：" + tuple.getValue() + "，score：" + tuple.getScore()));

        System.out.println("原来的候选科室 candidates 数量：" + candidates.size());

        // 本次体检所有应前往体检的科室
        Set<String> allPlaces = progressDTO.getAllPlaces().stream().map(String::trim).collect(Collectors.toSet());

        // 从所有科室中筛选出本次体检应该前往的所有科室
        // allPlaces 在原代码中 tuple.getValue() 改为了 placeId，这里测试是错的
        Set<ZSetOperations.TypedTuple<String>> filter = candidates.stream()
                .filter(tuple -> allPlaces.contains(tuple.getValue().trim()) && !finishedPlaces.containsKey(tuple.getValue()))
                .collect(Collectors.toSet());

        System.out.println("---------------------------------------------------------");
        System.out.println("筛选后的科室数量：" + filter.size());
        filter.forEach(tuple -> System.out.println("筛选后的候选科室：" + tuple.getValue() + "，score：" + tuple.getScore()));

    }

    /**
     * 增加减少排队人数
     */
    @Test
    void testQueueNum() {
        // 因为类上添加了 @SpringBootTest，启动时会启动整个 Spring Boot 应用上下文
        Double score = stringRedisTemplate.opsForZSet().incrementScore(FlowRegulationConstants.FLOW_PLACE_RANK, "骨密度检查室", 0);
        System.out.println("score：" + score);
    }

    @Test
    void testCheckinInsertCheckup() {

        // 3.1 筛选适合体检人性别的体检项目
        GoodsSnapshotEntity goodsSnapshotById = goodsSnapshotDao.getGoodsSnapshotById("694b933dabf6c43594f510fd");
        List<CheckupVO> checkupVOList = goodsSnapshotById.getCheckup().stream()
                .filter(checkup -> "无".equals(checkup.getSex()) || Objects.equals(checkup.getSex(), "男"))
                .collect(Collectors.toList());
        // 3.2 生成体检结果快照
        boolean insertResult = checkupResultDao.insert("165A1AB1725F4D25AFEE5B91AD5A04F3", "李煌", checkupVOList);

        System.out.println(insertResult);
    }

    @Test
    void testPlaceString() {
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(2);
        ids.add(3);
        ids.add(4);
        @SuppressWarnings("unchecked")
        List<String> entityList = flowRegulationDao
                .selectList(new LambdaQueryWrapper<FlowRegulationEntity>().select(FlowRegulationEntity::getPlace).in(FlowRegulationEntity::getId, ids))
                .stream().map(FlowRegulationEntity::getPlace).collect(Collectors.toList());

        String displayPlaces = entityList.size() > 3
                ? String.join("，", entityList.subList(0, 3)) + "，..."
                : String.join("，", entityList);
        System.out.println("删除失败，以下：【 " + displayPlaces + " 】 等 【 " + entityList.size() + " 】个科室" + "仍有排队人员");
    }
}
