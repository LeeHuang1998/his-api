package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.io.IoUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.exception.BizCodeEnum;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.exception.FileException;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.mis.dto.goods.request.GoodsPageRequest;
import com.leehuang.his.api.mis.dto.goods.request.GoodsRequest;
import com.leehuang.his.api.mis.dto.goods.request.RemoveImageRequest;
import com.leehuang.his.api.common.request.UpdateStatusRequest;
import com.leehuang.his.api.mis.dto.goods.vo.GoodsMisPageVO;
import com.leehuang.his.api.mis.dto.rule.vo.RulesGoodsVO;
import com.leehuang.his.api.mis.dto.goods.vo.GoodsDetailVO;
import com.leehuang.his.api.mis.service.MisGoodsService;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/mis/goods")
public class MisGoodsController {

    @Resource
    private MisGoodsService misGoodsService;

    /**
     * 分页查询商品数据
     * @param request   查询参数
     * @return          查询结果
     */
    @PostMapping("/getGoodsListByPage")
    @SaCheckPermission(value = {"ROOT", "GOODS:SELECT"}, mode = SaMode.OR)
    public R getGoodsListByPage(@RequestBody @Valid GoodsPageRequest request) {
        PageUtils<GoodsMisPageVO> pageData = misGoodsService.getGoodsListByPage(request);
        return R.OK().put("pageData", pageData);
    }

    /**
     * 弹窗上传商品图片
     * @param file  上传的文件
     * @return      上传地址
     */
    @PostMapping("/uploadImage")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R uploadImage(MultipartFile file) {
        String path = misGoodsService.uploadImage(file);
        return R.OK().put("path", path);
    }

    /**
     * 获取所有的促销方案
     * @return   所有的促销方案
     */
    @GetMapping("/getAllRules")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R getAllRules() {
        List<RulesGoodsVO> allRules = misGoodsService.getAllRules();
        return R.OK().put("ruleList", allRules);
    }

    /**
     * 新增体检套餐
     * @param request   体检套餐参数
     * @return          新增行数
     */
    @PostMapping("/insertGoods")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT"}, mode = SaMode.OR)
    public R insertGoods(@RequestBody @Valid GoodsRequest request) {
        int rows = misGoodsService.insertGoods(request);
        return R.OK().put("rows", rows);
    }

    /**
     * 根据 id 获取套餐商品信息，用于修改弹窗中回显商品信息
     * @param request       id，查询的套餐 id
     * @return              套餐信息
     */
    @PostMapping("/getGoodsById")
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R getGoodsById(@RequestBody @Valid IdRequest request){
        GoodsDetailVO goodsById = misGoodsService.getGoodsById(request.getId());
        return R.OK().put("goods", goodsById);
    }

    /**
     * 修改套餐商品信息
     * @param request       套餐商品信息参数
     * @return              修改行数
     */
    @PostMapping("/updateGoods")
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R updateGoods(@RequestBody @Valid GoodsRequest request) {
        int rows = misGoodsService.updateGoods(request);
        return R.OK().put("rows", rows);
    }

    /**
     * 上传 checkupExcel 文件，并更新数据库 中 chekcup 和 md5 字段
     * @param request       套餐 id
     * @param file          上传的文件
     * @return              更新行数
     */
    @PostMapping("/uploadCheckupExcel")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R uploadCheckupExcel(@Valid IdRequest request, @Param("file") MultipartFile file) {
        int rows = misGoodsService.uploadCheckupExcel(request, file);
        return R.OK().put("rows", rows);
    }

    /**
     * 生成文件下载的预签名URL
     * @param id 文件 ID 请求
     * @return 包含预签名URL的响应
     */
    @GetMapping("/generateDownloadUrl")
    @SaCheckPermission(value = {"ROOT", "GOODS:SELECT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R generateDownloadUrl(@RequestParam int id) throws FileException{
        System.out.println(id);
        // 生成预签名URL，设置过期时间（例如5分钟）
        String presignedUrl = misGoodsService.generatePresignedUrl(id);
        return R.OK().put("url", presignedUrl);
    }

    /**
     * 已过时，使用 generateDownloadUrl 替代
     * 下载 excel 文件，前端超链接只能发送 GET 请求，不需要返回，一旦开始向 HttpServletResponse 写入数据，就不应该再返回 R 对象，因为响应已经被提交了
     * @param request        套餐 id
     * @param response       响应
     */
    @Deprecated
    @GetMapping("/downloadCheckupExcel")
    @SaCheckPermission(value = {"ROOT", "GOODS:SELECT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public void downloadCheckupExcel(@Valid IdRequest request, HttpServletResponse response) {

        // 设置下载文件的名称
        response.setHeader("Content-Disposition", "attachment;filename=excle_" + request.getId() + ".xlsx");
        //该 MIME 类型会让浏览器弹出下载对话框
        response.setContentType("application/x-download");
        // 设置字符集编码
        response.setCharacterEncoding("UTF-8");

        // 获取套餐文档的输入流
        InputStream in = misGoodsService.downloadCheckupExcel(request);

        try (
                // 获取输入输出流
                BufferedInputStream bin = new BufferedInputStream(in);
                // 下载接口一旦把 response.getOutputStream() 打开，就不能再靠全局异常处理器，因为响应可能已经提交
                BufferedOutputStream bout = new BufferedOutputStream(response.getOutputStream())
                ){
            // 将数据写入到文件
            IoUtil.copy(bin, bout);
        } catch (IOException e) {
            throw new HisException(BizCodeEnum.HIS_EXCEPTION.getCode(), "网络/IO 错误", e);
        } catch (Exception e) {
            throw new HisException(BizCodeEnum.HIS_EXCEPTION.getCode(), "下载失败，出现其他异常错误", e);
        }
    }

    /**
     * 修改套餐商品的状态
     * @param request       套餐商品信息参数，id 和 status
     * @return              修改行数
     */
    @PostMapping("/updateGoodsStatus")
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R updateStatus(@RequestBody @Valid UpdateStatusRequest request) {
        int rows = misGoodsService.updateStatus(request);
        return R.OK().put("rows", rows);
    }

    /**
     * 删除套餐商品信息
     * @param request        ids，删除的套餐 id 列表
     * @return               删除行数
     */
    @PostMapping("/deleteGoods")
    @SaCheckPermission(value = {"ROOT", "GOODS:DELETE"}, mode = SaMode.OR)
    public R deleteGoods(@RequestBody @Valid IdsRequest request) {
        int rows = misGoodsService.deleteGoods(request.getIds());
        return R.OK().put("rows", rows);
    }

    /**
     * 直接在 minio 中删除图片
     * @param request   需要删除的图片路径数组
     * @return          删除行数
     */
    @PostMapping("/removeImages")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT", "GOODS:UPDATE", "GOODS:DELETE"}, mode = SaMode.OR)
    public R removeImages(@RequestBody @Valid RemoveImageRequest request) {
        misGoodsService.removeImages(request);
        return R.OK();
    }
}
