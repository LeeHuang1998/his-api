package com.leehuang.his.api.mis.mapper;

import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultItemVO;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultVO;
import com.leehuang.his.api.mis.dto.report.CheckupResultDTO;
import com.leehuang.his.api.mis.dto.report.ResultItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")          // componentModel = "spring"：生成实现类并加上 Spring 注解（@Component），就可以直接注入使用该实现类
public interface CheckupReportConverter {

    /**
     * PlaceCheckupResultVO → CheckupResultDTO
     * 字段名一致时才会自动映射，若不一致则需要注解说明
     * source：源对象中的字段名      target：转换后对象中对应的字段名     expression：转换该字段时调用该方法
     *
     */
    @Mapping(target = "date", source = "checkupDate")
    @Mapping(target = "resultItems", source = "checkupItems")
    @Mapping(target = "image", expression = "java(handleImage(vo))")
    CheckupResultDTO toCheckupResultDTO(PlaceCheckupResultVO vo);

    /**
     * 处理 image 字段
     */
    default String handleImage(PlaceCheckupResultVO vo) {
        if (vo == null) {
            return null;
        }
        if ("模板 2".equals(vo.getTemplate())) {
            return vo.getImage();
        }
        return null;
    }

    /**
     * List<PlaceCheckupResultVO> → List<CheckupResultDTO> 转换
     */
    List<CheckupResultDTO> toCheckupResultDTOList(List<PlaceCheckupResultVO> list);

    /**
     * PlaceCheckupResultItemVO → ResultItemDTO
     */
    ResultItemDTO toResultItemDTO(PlaceCheckupResultItemVO vo);


    /**
     * List<PlaceCheckupResultItemVO> → List<ResultItemDTO> 转换
     */
    List<ResultItemDTO> toResultItemDTOList(List<PlaceCheckupResultItemVO> list);
}