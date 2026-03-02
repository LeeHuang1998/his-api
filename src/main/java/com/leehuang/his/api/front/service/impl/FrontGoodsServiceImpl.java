package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.db.dao.GoodsDao;
import com.leehuang.his.api.db.dao.GoodsSnapshotDao;
import com.leehuang.his.api.db.dao.OrderDao;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.db.pojo.GoodsSnapshotEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.goods.FrontGoodsDataDTO;
import com.leehuang.his.api.front.dto.goods.request.GoodsListPageRequest;
import com.leehuang.his.api.front.dto.goods.vo.GoodsSnapshotVO;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;
import com.leehuang.his.api.front.service.FrontGoodsService;
import com.leehuang.his.api.front.dto.goods.vo.GoodsPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service("FrontGoodsServiceImpl")
@RequiredArgsConstructor
public class FrontGoodsServiceImpl implements FrontGoodsService {

    // 使用 @RequiredArgsConstructor 构造函数注入，并用 final 修饰的原因：
    //      1. 不可变性：final 字段一旦被赋值就不能再被修改，避免在多线程环境下被其他线程修改，提高安全性
    //      2. 强制完全初始化：final 字段必须在构造器结束前被初始化，这保证了对象一旦创建成功，所有必需的依赖都已经就位
    //      3. 明确设计意图：使用 final 明确表达了这些依赖是对象正常工作所必需的，而不是可选的或可变的
    //      4. 促进更好的设计：强制通过构造器注入所有必需依赖，促进了面向对象设计的最佳实践。
    // 不使用 final 的场景：
    //      1. 可变对象：如果对象需要被修改，那么就不应该使用 final 修饰
    //      2. 可选依赖：如果依赖是可选的，那么就不应该使用 final 修饰
    //      3. 循环依赖：虽然循环依赖本身是设计问题，但在某些遗留系统中可能需要使用 setter 注入来解决


    private final GoodsDao goodsDao;

    // minio 配置
    private final MinioProperties minioProperties;

    private final GoodsSnapshotDao goodsSnapshotDao;
    private final OrderDao orderDao;

    /**
     * 根据 id 获取商品页面信息，缓存空间名称为 goods，key 为 id
     * @param id    商品 id
     * @return      商品页面信息
     */
    @Override
    @Cacheable(cacheNames = "goods", key = "#id")
    public GoodsPageVO getGoodsFrontPageVoById(int id) {
        FrontGoodsDataDTO dto =  goodsDao.getGoodsFrontPageVoById(id, true);

        if (dto == null){
            throw new HisException("商品不存在");
        }

        GoodsPageVO pageVO = new GoodsPageVO();

        String[] ignoreProperties = {"images", "tags", "checkup1", "checkup2", "checkup3", "checkup4"};
        BeanUtil.copyProperties(dto, pageVO, ignoreProperties);

        // 转换数据格式
        // 将 JSON 数组转换为 List，并保存到对象中
        if (dto.getCheckup1() != null){
            pageVO.setCheckup_1(JSON.parseObject(dto.getCheckup1(), new TypeReference<>() {}));
        }
        if (dto.getCheckup2() != null){
            pageVO.setCheckup_2(JSON.parseObject(dto.getCheckup2(), new TypeReference<>() {}));
        }
        if (dto.getCheckup3() != null){
            pageVO.setCheckup_3(JSON.parseObject(dto.getCheckup3(), new TypeReference<>() {}));
        }
        if (dto.getCheckup4() != null){
            pageVO.setCheckup_4(JSON.parseObject(dto.getCheckup4(), new TypeReference<>() {}));
        }

        // 转换 tags 和 images
        if (dto.getImages() != null){

            List<String> imagesString = JSON.parseArray(dto.getImages(), String.class);

            //  为每个元素构造完整的图片地址再转换为 String[]，new String[0] 表示创建一个长度为 0 的 String 数组，当长度不够时会重新创建一个长度正确的新数组
            pageVO.setImages(
                imagesString.stream().map(image -> {
                    return minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + image;
                }).toArray(String[]::new)
            );
        }

        if (dto.getTags() != null){
            List<String> tagString = JSON.parseArray(dto.getTags(), String.class);
            pageVO.setTags(tagString.toArray(new String[0]));
        }

        return pageVO;
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public PageUtils<GoodsItemVO> searchGoodsListByPage(GoodsListPageRequest request) {

        // 获取分页起始记录 id
        Integer page = request.getPage();
        int start = (page - 1) * request.getLength();

        // 根据条件分页查询
        List<GoodsItemVO> goodsList = goodsDao.searchGoodsByPage(start, request).stream().peek(goods -> {
            // 设置 img 封面图片地址
            String img = minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + JSON.parseObject(goods.getImage(), String[].class)[0];
            goods.setImage(img);
        }).collect(Collectors.toList());

        // 根据条件查询总数
        int total = goodsDao.searchCountByPage(request);

        return new PageUtils<>(total, request.getLength(), request.getPage(), goodsList);
    }

    /**
     * 获取商品快照信息
     * @param snapshotId
     * @param customerId
     * @return
     */
    @Override
    public GoodsSnapshotVO searchSnapshotById(String snapshotId, Integer customerId) {
        // 若 customerId 不为空，检查该客户是否拥有该订单快照
        if (customerId != null) {
            // 判断用户是否购买过该商品
            Long count = orderDao.selectCount(new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getCustomerId, customerId)
                    .eq(OrderEntity::getSnapshotId, snapshotId));
            if (count == 0) {
                throw new HisException("该用户未购买该商品");
            }
        }

        // 获取快照信息
        GoodsSnapshotEntity snapShot = goodsSnapshotDao.getGoodsSnapshotById(snapshotId);

        // 将查询的商品快照转换为 VO 类
        GoodsSnapshotVO goodsSnapshotVO = new GoodsSnapshotVO();

        BeanUtil.copyProperties(snapShot, goodsSnapshotVO);

        goodsSnapshotVO.setImage(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + snapShot.getImage());

        return goodsSnapshotVO;
    }
}
