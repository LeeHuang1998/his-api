package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.GoodsEntity;
import com.leehuang.his.api.db.entity.RuleEntity;
import com.leehuang.his.api.db.pojo.GoodsSnapShotDTO;
import com.leehuang.his.api.db.pojo.GoodsSnapshotEntity;
import com.leehuang.his.api.front.dto.goods.FrontGoodsDataDTO;
import com.leehuang.his.api.front.dto.goods.request.GoodsListPageRequest;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;
import com.leehuang.his.api.mis.dto.goods.request.GoodsPageRequest;
import com.leehuang.his.api.mis.dto.goods.vo.GoodsMisPageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_goods(体检套餐表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.GoodsEntity
*/
public interface GoodsDao extends BaseMapper<GoodsEntity> {

    // 分页查询 goods 数据
    List<GoodsMisPageVO> getGoodsListByPage(@Param("start") int start, @Param("request") GoodsPageRequest request);

    // 获取分页查询 goods 总数
    long getGoodsListCount(GoodsPageRequest request);

    // 获取所有促销方案
    List<RuleEntity> getAllRules();

    // 插入新套餐
    int insertGoods(GoodsEntity goodsEntity);

    // 根据 id 查询商品数据
    GoodsEntity getGoodsById(@Param("id") int id);

    // 更新套餐信息
    int updateGoods(GoodsEntity goodsEntity);

    // 更新 checkup 和 md5 字段
    int updateGoodsCheckup(@Param("id") int id, @Param("checkup") String checkup, @Param("md5") String md5);

    // 更新套餐上架状态
    int updateGoodsStatus(@Param("id") Integer id, @Param("status") Boolean status);

    // 根据 id 数组查询商品是否可以被删除
    Boolean selectCanDelGoods(@Param("ids") Integer[] ids);

    // 查询需要删除套餐的图片
    List<String> selectImage(@Param("ids") Integer[] ids);

    // 删除套餐
    int deleteGoods(@Param("ids") Integer[] ids);

    // 根据 id 获取商品页面信息
    FrontGoodsDataDTO getGoodsFrontPageVoById(@Param("id") int id, @Param("status") boolean status);

    // 获取商品上架状态
    Boolean selectStatus(@Param("id") Integer id);

    // 获取分区商品
    List<GoodsItemVO> searchGoodsByPartIds(@Param("ids") Integer[] ids);

    // 分页查询商品
    List<GoodsItemVO> searchGoodsByPage(@Param("start") int start, @Param("request") GoodsListPageRequest request);

    // 分页查询数据总数
    int searchCountByPage(@Param("request") GoodsListPageRequest request);

    // 获取商品快照信息
    GoodsSnapShotDTO searchGoodsSnapshotById(@Param("id") Integer id);
}




