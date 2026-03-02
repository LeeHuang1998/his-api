package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leehuang.his.api.common.utils.MinioUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.GoodsEntity;
import com.leehuang.his.api.db.entity.RuleEntity;
import com.leehuang.his.api.db.dao.GoodsDao;
import com.leehuang.his.api.exception.BizCodeEnum;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.exception.FileException;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.mis.dto.goods.request.GoodsRequest;
import com.leehuang.his.api.mis.dto.goods.request.GoodsPageRequest;
import com.leehuang.his.api.mis.dto.goods.request.RemoveImageRequest;
import com.leehuang.his.api.common.request.UpdateStatusRequest;
import com.leehuang.his.api.mis.dto.goods.vo.GoodsMisPageVO;
import com.leehuang.his.api.mis.dto.rule.vo.RulesGoodsVO;
import com.leehuang.his.api.mis.dto.goods.vo.GoodsDetailVO;
import com.leehuang.his.api.mis.service.MisGoodsService;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service("MisGoodsService")
@RequiredArgsConstructor
public class MisGoodsServiceImpl implements MisGoodsService {

    private final GoodsDao goodsDao;

    private final MinioUtil minioUtil;

    private final ObjectMapper objectMapper;

    /**
     * 分页查询商品数据
     * @param request   查询参数
     * @return          查询到的数据结果，包装到 PageUtils 中返回
     */
    @Override
    public PageUtils<GoodsMisPageVO> getGoodsListByPage(GoodsPageRequest request) {
        // 获取分页起始记录 id
        Integer page = request.getPage();
        int start = (page - 1) * request.getLength();
        // 获取商品数据
        List<GoodsMisPageVO> goodsListByPage = goodsDao.getGoodsListByPage(start, request);

        // 获取总行数
        long totalCount = goodsDao.getGoodsListCount(request);
        return new PageUtils<>(totalCount, request.getLength(), request.getPage(), goodsListByPage);
    }

    /**
     * 上传图片
     * @param file  图片文件
     * @return      图片上传后的路径
     */
    @Override
    public String uploadImage(MultipartFile file) {
        // 生成新的文件名
        String fileName = IdUtil.simpleUUID() + ".jpg";
        String path = "front/goods/goods_" + fileName;

        minioUtil.uploadImage(path, file);
        return path;
    }

    /**
     * 获取所有促销方案
     * @return   促销方案列表
     */
    @Override
    public List<RulesGoodsVO> getAllRules() {
        List<RuleEntity> allRules = goodsDao.getAllRules();

        // @Data 只有当字段中没有 final 或 @NotNull 时才会生成无参构造，若使用全参构造注解则需要显式的添加无参构造
        return allRules.stream().map(ruleEntity -> new RulesGoodsVO(ruleEntity.getId(), ruleEntity.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 新增体检套餐
     * @param request   新套餐参数
     * @return          插入的行数
     */
    @Override
    @Transactional
    public int insertGoods(GoodsRequest request) {
        GoodsEntity goodsEntity = new GoodsEntity();

        // 复制时需要忽略的属性
        String[] ignoreProperties = {"images", "checkup_1", "checkup_2", "checkup_3", "checkup_4", "tags"};
        BeanUtil.copyProperties(request, goodsEntity, ignoreProperties);

        // 将 6 个字段转换为 JSON 数组
        // 设置 image，所有的图片路径
        goodsEntity.setImage(JSON.toJSONString(request.getImages()));
        try {
            goodsEntity.setImage(objectMapper.writeValueAsString(request.getImages()));

            // 所有的体检项目
            if (request.getCheckup_1() != null && !request.getCheckup_1().isEmpty()) {
                goodsEntity.setCheckup1(objectMapper.writeValueAsString(request.getCheckup_1()));
            }
            if (request.getCheckup_2() != null && !request.getCheckup_2().isEmpty()) {
                goodsEntity.setCheckup2(objectMapper.writeValueAsString(request.getCheckup_2()));
            }
            if (request.getCheckup_3() != null && !request.getCheckup_3().isEmpty()) {
                goodsEntity.setCheckup3(objectMapper.writeValueAsString(request.getCheckup_3()));
            }
            if (request.getCheckup_4() != null && !request.getCheckup_4().isEmpty()) {
                goodsEntity.setCheckup4(objectMapper.writeValueAsString(request.getCheckup_4()));
            }

            // tag
            if (request.getTags() != null && request.getTags().length > 0 ) {
                goodsEntity.setTag(objectMapper.writeValueAsString(request.getTags()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 生成 md5 值
        String md5 = generateMd5(goodsEntity);
        goodsEntity.setMd5(md5);

        // 插入数据到数据库中
        return goodsDao.insertGoods(goodsEntity);
    }

    /**
     * 根据 id 查询商品数据
     * @param id    商品 id
     * @return      商品数据
     */
    @Override
    public GoodsDetailVO getGoodsById(int id) {
        GoodsEntity goodsById = goodsDao.getGoodsById(id);
        GoodsDetailVO goodsVo = new GoodsDetailVO();

        String[] ignoreProperties = {"image", "checkup1", "checkup2", "checkup3", "checkup4", "tag"};
        BeanUtil.copyProperties(goodsById, goodsVo, ignoreProperties);

        // 将 JSON 数组转换为 List，并保存到对象中
        if (goodsById.getCheckup1() != null){
            goodsVo.setCheckup_1(JSON.parseObject(goodsById.getCheckup1(), new TypeReference<>() {}));
        }
        if (goodsById.getCheckup2() != null){
            goodsVo.setCheckup_2(JSON.parseObject(goodsById.getCheckup2(), new TypeReference<>() {}));
        }
        if (goodsById.getCheckup3() != null){
            goodsVo.setCheckup_3(JSON.parseObject(goodsById.getCheckup3(), new TypeReference<>() {}));
        }
        if (goodsById.getCheckup4() != null){
            goodsVo.setCheckup_4(JSON.parseObject(goodsById.getCheckup4(), new TypeReference<>() {}));
        }

        // images
        if (goodsById.getImage() != null){
            List<String> imagesString = JSON.parseArray(goodsById.getImage(), String.class);
            // 再转换为 String[]，new String[0] 表示创建一个长度为 0 的 String 数组，当长度不够时会重新创建一个长度正确的新数组
            goodsVo.setImages(imagesString.toArray(new String[0]));
        }

        // tags
        if (goodsById.getTag() != null){
            List<String> tagString = JSON.parseArray(goodsById.getTag(), String.class);
            goodsVo.setTags(tagString.toArray(new String[0]));
        }

        return goodsVo;
    }

    /**
     * 更新商品数据，修改时需要使缓存失效
     * @param request   更新的套餐商品参数
     * @return          更新行数
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "goods", key = "#request.id")
    public int updateGoods(GoodsRequest request) {
        GoodsEntity goodsEntity = new GoodsEntity();

        // 复制时需要忽略的属性
        String[] ignoreProperties = {"images", "checkup_1", "checkup_2", "checkup_3", "checkup_4", "tags"};
        BeanUtil.copyProperties(request, goodsEntity, ignoreProperties);

        // 将 6 个字段转换为 JSON 数组
        goodsEntity.setImage(JSON.toJSONString(request.getImages()));

        if (request.getCheckup_1() != null) {
            goodsEntity.setCheckup1(JSON.toJSONString(request.getCheckup_1()));
        }
        if (request.getCheckup_2() != null) {
            goodsEntity.setCheckup2(JSON.toJSONString(request.getCheckup_2()));
        }
        if (request.getCheckup_3() != null) {
            goodsEntity.setCheckup3(JSON.toJSONString(request.getCheckup_3()));
        }
        if (request.getCheckup_4() != null) {
            goodsEntity.setCheckup4(JSON.toJSONString(request.getCheckup_4()));
        }
        if (request.getTags() != null) {
            goodsEntity.setTag(JSON.toJSONString(request.getTags()));
        }

        // 生成 md5 值
        String md5 = generateMd5(goodsEntity);
        goodsEntity.setMd5(md5);
        return goodsDao.updateGoods(goodsEntity);
    }

    /**
     * 上传体检套餐对应的 checkupExcel，更新后需要使缓存失效
     * @param request       体检套餐 id
     * @param file          excel 文件
     * @return             是否上传成功，成功后返回 1，否则返回 0
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "goods", key = "#request.id")
    public int uploadCheckupExcel(IdRequest request, MultipartFile file) {
        // 创建 List 用于存储 excel 的行数据
        List<LinkedHashMap<String, String>> rowList = new ArrayList<>();

        // 获取文件的 IO 流，在 try 的括号里声明的 I/O 流，不需要、也不应该手动调用 close()
        try (
            // 将 io 流包装为 buffered 缓冲流，提高读取效率并减少磁盘或网络的直接访问次数
            InputStream in = file.getInputStream();
            BufferedInputStream bin = new BufferedInputStream(in);

            // 从 io 流中获取 excel 文件对象，XSSFWorkbook 若没有放到 try-with-resources 里，异常时可能泄漏文件句柄
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(bin);
        ) {

            // 由于该 excel 文件中只有一个 Sheet 页，取出该页
            XSSFSheet sheet = xssfWorkbook.getSheetAt(0);

            // 循环取出行内数据，第一行为表头，需要忽略，从第二行开始取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);

                // 共有八列，取出每列对应的数据，getStringCellValue() 当列是数值或空时会抛异常
                XSSFCell cell_1 = row.getCell(0);
                String value_1 = cell_1.getStringCellValue() == null ? "" : cell_1.getStringCellValue();

                XSSFCell cell_2 = row.getCell(1);
                String value_2 = cell_2.getStringCellValue() == null ? "" : cell_2.getStringCellValue().trim();

                XSSFCell cell_3 = row.getCell(2);
                String value_3 = cell_3.getStringCellValue() == null ? "" : cell_3.getStringCellValue().trim();

                XSSFCell cell_4 = row.getCell(3);
                String value_4 = cell_4.getStringCellValue() == null ? "" : cell_4.getStringCellValue().trim();

                XSSFCell cell_5 = row.getCell(4);
                String value_5 = cell_5.getStringCellValue() == null ? "" : cell_5.getStringCellValue().trim();

                XSSFCell cell_6 = row.getCell(5);
                String value_6 = cell_6.getStringCellValue() == null ? "" : cell_6.getStringCellValue().trim();

                XSSFCell cell_7 = row.getCell(6);
                String value_7 = cell_7.getStringCellValue() == null ? "" : cell_7.getStringCellValue().trim();

                XSSFCell cell_8 = row.getCell(7);
                String value_8 = cell_8.getStringCellValue() == null ? "" : cell_8.getStringCellValue().trim();

                // 将数据存入 LinkedHashMap 中，使用 LinkHashMap 是为了保存存入的顺序，一个 map 对象对应一行数据
                LinkedHashMap<String, String> rowMap = new LinkedHashMap<>();
                rowMap.put("place", value_1);
                rowMap.put("name", value_2);
                rowMap.put("item", value_3);
                rowMap.put("type", value_4);
                rowMap.put("code", value_5);
                rowMap.put("sex", value_6);
                rowMap.put("value", value_7);
                rowMap.put("template", value_8);

                // 将 rowMap 存入到 List 中
                rowList.add(rowMap);
            }

            // 将 excel 文件存入到 minio 服务器中
            String path = "/mis/goods/checkup/excel_" + request.getId() + ".xlsx";
            minioUtil.uploadExcel(path, file);

            // 从数据库中查询该套餐的数据，并修改 md5 的值
            GoodsEntity goodsById = goodsDao.getGoodsById(request.getId());
            // 设置 checkup 的值，将 List 转换为 JSON 数组
            String checkup = JSON.toJSONString(rowList);
            // 生成新的 md5 值
            goodsById.setCheckup(checkup);
            String md5 = generateMd5(goodsById);

            if (rowList.isEmpty()) {
                throw new FileException(BizCodeEnum.UPLOAD_EXCEPTION.getCode(), "文档内容无效");
            }

            // 更新到数据库中
            return goodsDao.updateGoodsCheckup(request.getId(), checkup, md5);
        } catch (FileException e) {
            throw new FileException( e.getCode(), "上传体检 Excel 失败，goodsId=" + request.getId(), e);
        } catch (IOException e) {
            throw new HisException(BizCodeEnum.HIS_EXCEPTION.getCode(), "网络/IO 错误", e);
        }
    }

    /**
     * 生成预签名 url，过期时间设置为 5 分钟
     * @param id         套餐 id
     * @return           预签名 url
     */
    @Override
    public String generatePresignedUrl(Integer id) {
        // 查询数据库中该商品文档是否为空
        GoodsEntity goodsById = goodsDao.getGoodsById(id);
        if (goodsById.getCheckup() == null) {
            throw new FileException(BizCodeEnum.DOWNLOAD_EXCEPTION.getCode(), "该套餐没有上传文档");
        } else {
            String path = "/mis/goods/checkup/excel_" + id + ".xlsx";
            return minioUtil.generatePresignedUrl(path, 5);
        }
    }

    /**
     * 已过时，使用 generatePresignedUrl 替代
     * 获取下载的套餐文档的输入流
     * @param request        套餐 id
     * @return               文件输入流
     */
    @Deprecated
    @Override
    public InputStream downloadCheckupExcel(IdRequest request) {
        String path = "/mis/goods/checkup/excel_" + request.getId() + ".xlsx";
        return minioUtil.downloadExcel(path);
    }

    /**
     * 更新套餐上架状态，当下架时删除缓存
     * @param request    请求参数
     * @return           更新结果
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "goods", key = "#request.id", condition = "#request.status == false ")
    public int updateStatus(UpdateStatusRequest request) {
        return goodsDao.updateGoodsStatus(request.getId(), request.getStatus());
    }

    /**
     * 删除套餐，同时删除 redis 中的缓存
     * @param ids    套餐 id 数组
     * @return       删除结果
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "goods", key = "#ids")
    public int deleteGoods(Integer[] ids) {
        // 查询商品是否可以被删除
        Boolean canDel = goodsDao.selectCanDelGoods(ids);
        if (canDel) {
            // 批量删除商品图片
            List<String> pathList = new ArrayList<>();
            // 1. 查询对应的图片
            List<String> imageList = goodsDao.selectImage(ids);
            // 2. 将图片路径存入到 pathList 中
            for (String s : imageList) {
                List<String> strings = JSON.parseArray(s, String.class);
                pathList.addAll(strings);
            }
            // 3. 从 minio 中批量删除图片
            List<DeleteObject> deleteObjList
                    = pathList.stream().map(DeleteObject::new).collect(Collectors.toList());
            minioUtil.removeImages(deleteObjList);

            // 删除所有的商品
            return goodsDao.deleteGoods(ids);
        } else {
            throw new HisException(BizCodeEnum.HIS_EXCEPTION.getCode(), "存在不满足被删除条件的商品，无法删除");
        }
    }

    /**
     * 直接在 minio 中删除图片
     * @param request   需要删除图片的路径数组
     */
    @Override
    public void removeImages(RemoveImageRequest request) {
        // 将数据转换为 List
        List<String> imagesList = Arrays.stream(request.getRemoveImages()).collect(Collectors.toList());
        // 批量删除图片
        List<DeleteObject> objectList
                = imagesList.stream().map(DeleteObject::new).collect(Collectors.toList());
        this.minioUtil.removeImages(objectList);
    }

    /**
     * 生成商品对应的 md5
     * @param goodsEntity   商品实体
     * @return              商品对应的 md5
     */
    private String generateMd5(GoodsEntity goodsEntity) {
        JSONObject json = (JSONObject) JSON.toJSON(goodsEntity);

        // 不属于计算商品 MD5 值的字段
        String[] fieldsToRemove = {"id", "partId", "salesVolume", "status", "md5", "updateTime", "createTime"};

        // 去除不必要的字段
        for (String field : fieldsToRemove) {
            json.remove(field);
        }

        // 生成 md5
        return MD5.create().digestHex(json.toJSONString().toUpperCase());
    }

    /**
     * 获取分区商品
     * @param ids           分区 id 数组
     * @return              分区商品集合
     */
    @Override
    public List<GoodsItemVO> getGoodsByPartIds(Integer[] ids) {
        return goodsDao.searchGoodsByPartIds(ids);
    }

    /**
     * 根据规则 id 查询
     * @param ruleId
     * @return
     */
    @Override
    public Long searchGoodsCountByRuleId(Integer ruleId) {
        return goodsDao.selectCount(new LambdaQueryWrapper<GoodsEntity>().eq(GoodsEntity::getRuleId, ruleId));
    }
}
