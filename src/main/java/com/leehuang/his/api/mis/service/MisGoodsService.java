package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.mis.dto.goods.request.GoodsRequest;
import com.leehuang.his.api.mis.dto.goods.request.GoodsPageRequest;
import com.leehuang.his.api.mis.dto.goods.request.RemoveImageRequest;
import com.leehuang.his.api.common.request.UpdateStatusRequest;
import com.leehuang.his.api.mis.dto.goods.vo.GoodsMisPageVO;
import com.leehuang.his.api.mis.dto.rule.vo.RulesGoodsVO;
import com.leehuang.his.api.mis.dto.goods.vo.GoodsDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface MisGoodsService {

    // 分页查询商品数据
    PageUtils<GoodsMisPageVO> getGoodsListByPage(GoodsPageRequest request);

    // 上传图片
    String uploadImage(MultipartFile file);

    // 获取所有促销方案
    List<RulesGoodsVO> getAllRules();

    // 插入新套餐
    int insertGoods(GoodsRequest request);

    // 根据 id 查询套餐数据
    GoodsDetailVO getGoodsById(int id);

    // 更新套餐商品数据
    int updateGoods(GoodsRequest request);

    // 上传套餐的 CheckupExcel（即体检内容 excel）
    int uploadCheckupExcel(IdRequest id, MultipartFile file);

    // 获取下载的套餐文档的输入流
    InputStream downloadCheckupExcel(IdRequest id);

    // 更新套餐上架状态
    int updateStatus(UpdateStatusRequest request);

    // 生成预签名 url
    String generatePresignedUrl(Integer id);

    // 删除套餐
    int deleteGoods(Integer[] ids);

    // 直接在 minio 中删除图片
    void removeImages(RemoveImageRequest request);

    // 获取分区商品
    List<GoodsItemVO> getGoodsByPartIds(Integer[] ids);

    // 根据规则 id 查询商品
    Long searchGoodsCountByRuleId(Integer ruleId);
}
