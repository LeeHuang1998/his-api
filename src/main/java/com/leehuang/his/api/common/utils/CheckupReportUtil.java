package com.leehuang.his.api.common.utils;

import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.report.CheckupItemDTO;
import com.leehuang.his.api.mis.dto.report.CheckupReportDTO;
import com.leehuang.his.api.mis.dto.report.CheckupResultDTO;
import com.leehuang.his.api.mis.dto.report.ResultItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Component;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

@Component
@Slf4j
public class CheckupReportUtil {

    /**
     * 生成完整的体检报告文档
     * @param dto               体检报告数据（使用实体类封装）
     * @return                  生成的 Word 文档对象
     */
    public XWPFDocument createReport(CheckupReportDTO dto) throws Exception {
        XWPFDocument doc = new XWPFDocument();

        // 设置页面边距
        setPageMargins(doc);

        // 设置页脚（页码）
        createFooter(doc);

        // 依次构建文档各部分
        createCover(doc, dto);                                      // 封面
        createWelcome(doc, dto.getName(), dto.getSex());            // 欢迎语
        createCustomerInfo(doc, dto);                               // 体检人信息
        createCheckup(doc, dto.getCheckup());                       // 体检项目列表

        // 体检结果（按模板分别处理）
        for (CheckupResultDTO resultDto : dto.getResult()) {
            if ("模板1".equals(resultDto.getTemplate())) {
                createCheckupResultByTemplate1(doc, resultDto);
            } else if ("模板2".equals(resultDto.getTemplate())) {
                createCheckupResultByTemplate2(doc, resultDto);
            }
        }

        return doc;
    }

    /**
     * 设置文档页边距
     * @param doc       word 文档对象
     */
    private void setPageMargins(XWPFDocument doc) {
        // 获取文档的 body 部分，并添加一个新的节属性（SectPr）。每个 Word 文档至少有一个节，节属性包含页面设置（如边距、页眉页脚等）
        // doc.getDocument() 获取 Word 文档的底层根 XML 元素（返回 CTDocument），
        // .getBody() 从 CTDocument 对象中获取文档的主体部分
        // .addNewSectPr() 在 body 上添加一个新的节属性，用于控制页面格式
        CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();                           // 获取专门用于设置页边距格式的对象

        // 设置上下左右页边距
        pageMar.setLeft(BigInteger.valueOf(1200));
        pageMar.setRight(BigInteger.valueOf(1200));
        pageMar.setTop(BigInteger.valueOf(1000));
        pageMar.setBottom(BigInteger.valueOf(1000));
    }

    /**
     * 设置页脚页码
     * @param doc   word 文档对象
     */
    private void createFooter(XWPFDocument doc) {
        // 创建一个默认类型的页脚（与第一节关联）。页脚将出现在文档每一页的底部
        XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);

        // 创建一个段落，并设置其对齐方式为居中，使页码显示在页面底部中间
        XWPFParagraph paragraph = footer.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        // 插入页码域，setInstr("PAGE \\* MERGEFORMAT") 设置域指令，"PAGE \\* MERGEFORMAT" 表示插入当前页码
        paragraph.getCTP().addNewFldSimple().setInstr("PAGE \\* MERGEFORMAT");
        // 必须添加一个空的 run 才能生效，否则页码域可能无法正确显示
        paragraph.createRun();
    }

    /**
     * 设置封面页数据
     * @param doc               word 文档对象
     * @param dto               报告数据传输对象，包含体检报告的所有数据
     */
    private void createCover(XWPFDocument doc, CheckupReportDTO dto) throws Exception {
        // 1. 顶部中文全称
        XWPFParagraph para = createParagraph(doc, "智慧大健康体检中心", 20, true, ParagraphAlignment.LEFT);
        // 设置下划线样式
        para.setBorderBottom(Borders.THICK);
        // 设置行高为 480
        setParagraphSpacing(para,null, null, 600);

        // 2. 顶部英文全称
        para = createParagraph(doc, "Grand Health Examination Center", 10, false, ParagraphAlignment.LEFT);
        // 设置行高为 280
        setParagraphSpacing(para,null, null, 400);

        // 3. 报告标题
        para = createParagraph(doc, "体检报告书", 26, true, ParagraphAlignment.CENTER);
        // 设置段前段后距为
        setParagraphSpacing(para,2000, 2000, null);

        // 4. 二维码
        QrConfig config = new QrConfig();
        config.setWidth(150);
        config.setHeight(150);
        config.setMargin(2);
        // 生成二维码图片 BufferedImage 对象（内存中的图像）
        BufferedImage qrImage = QrCodeUtil.generate(dto.getUuid(), config);

        // 将二维码 BufferedImage 转为输入流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 将二维码编码为 PNG 格式，并写入字节数组输出流
        boolean written = ImageIO.write(qrImage, "png", baos);
        if (!written) {
            throw new HisException("无法将二维码编码为 PNG 格式");
        }

        // 将字节数组输出流中的数据转换为字节数组输入流，用于插入图片
        try (InputStream in = new ByteArrayInputStream(baos.toByteArray())) {
            // 将二维码插入到文档中
            insertImage(doc, in, ParagraphAlignment.CENTER, 1600, 150, 150, Document.PICTURE_TYPE_PNG);
        }

        // 5. 基本信息表格（4 行 2列，显示姓名、性别、体检中心地址、日期）
        XWPFTable table = doc.createTable(4, 2);
        // 设置表格为垂直居中
        table.setTableAlignment(TableRowAlign.CENTER);
        // 移除表格边框
        table.getCTTbl().getTblPr().unsetTblBorders();

        // 填充表格数据
        String[][] coverData = {
                {"姓   名：", dto.getName()},
                {"性   别：", dto.getSex()},
                {"门店地址：", dto.getAddress()},
                {"日   期：", dto.getAppointmentDate().toString()}
        };

        // 设置封面页的信息到表格中
        for (int i = 0; i < coverData.length; i++) {
            XWPFTableRow row = table.getRow(i);     // 获取第 i 行，行高设置为 600
            row.setHeight(600);

            // 第一列（标签）
            XWPFTableCell cellLabel = row.getCell(0);
            setCellText(cellLabel, coverData[i][0], 9, ParagraphAlignment.LEFT, null);

            // 第二列（值），设置列宽为 2800
            XWPFTableCell cellValue = row.getCell(1);
            setCellText(cellValue, coverData[i][1], 9, ParagraphAlignment.LEFT, null);
            setCellWidth(cellValue, 2800);

            // 为第二列的段落添加下边框
            cellValue.getParagraphArray(0).setBorderBottom(Borders.BABY_RATTLE);
        }
    }

    /**
     * 创建欢迎语段落
     * @param doc   word 文档对象
     * @param name  体检人姓名
     * @param sex   体检人性别
     */
    private void createWelcome(XWPFDocument doc, String name, String sex) {
        // 设置该段落强制从新的一页开始
        doc.createParagraph().createRun().addBreak(BreakType.PAGE);
        // 1. 创建标题
        XWPFParagraph para = createParagraph(doc, "健康体检报告", 18, false, ParagraphAlignment.CENTER);
        // 设置段前段后距和行距
        setParagraphSpacing(para, 0, 200, 420);
        // 2. 称呼行
        String salute = "尊敬的" + name + (sex.equals("男") ? "先生" : "女士") + "，您好！";
        para = createParagraph(doc, salute, 12, false, ParagraphAlignment.LEFT);
        setParagraphSpacing(para, null, 50, 360);

        // 3. 感谢语（首行缩进两个汉字约等于 600）
        para = createParagraph(doc, "感谢您到智慧大健康中心体检。现将您的体检结果汇总如下，请您认真阅读体检结果和建议。如有疑问，请您来院或者致电本中心服务电话010-1234567，我们将安排专业人员为您答疑解惑。欢迎对我们的工作提出批评和建议。祝您健康！",
                11, false, ParagraphAlignment.LEFT);
        para.setIndentationFirstLine(600);
        setParagraphSpacing(para, null, 100, 340);
    }

    /**
     * 体检人信息表格
     * @param doc   word 文档对象
     * @param dto   体检人信息
     */
    private void createCustomerInfo(XWPFDocument doc, CheckupReportDTO dto) {
        // 1. 表格标题
        XWPFParagraph para = createParagraph(doc, "体检人信息", 14, false, ParagraphAlignment.LEFT);
        setParagraphSpacing(para, 200, 100, null);

        // 2. 创建 4 行 6 列的表格，设置表格宽度为 9850
        XWPFTable table = doc.createTable(4, 6);
        CTTblPr tblPr = table.getCTTbl().getTblPr();        // 获取表格 XML 对象，再获取表格属性对象
        tblPr.getTblW().setType(STTblWidth.DXA);            // 设置表格宽度单位为 DXA（twips），宽度为 9850 twips
        tblPr.getTblW().setW(BigInteger.valueOf(9850));

        // 3. 填充表格数据
        // 第一行：姓名、性别、出生日期
        fillCustomerRow(table.getRow(0), new String[]{
                "姓名", dto.getName(),
                "性别", dto.getSex(),
                "出生日期", dto.getBirthday().toString()
        }, new Integer[]{1500, null, 1500, 1200, 1500, null}); // 设置部分列宽

        // 第二行：电话、年龄、体检日期
        fillCustomerRow(table.getRow(1), new String[]{
                "电话", dto.getTel(),
                "年龄", dto.getAge().toString(),
                "体检日期", dto.getAppointmentDate().toString()
        }, null);

        // 第三行：体检门店地址（第一列单元格为标签，其他单元格合并为一格）
        XWPFTableRow row2 = table.getRow(2);
        row2.setHeight(550);
        fillMergedRow(row2, "体检门店地址", dto.getAddress(), 5);

        // 第四行：体检套餐名称（第一列单元格为标签，其他单元格合并为一格）
        XWPFTableRow row3 = table.getRow(3);
        row3.setHeight(600);
        fillMergedRow(row3, "体检套餐", dto.getGoods(), 5);
    }

    /**
     * 填充表格行（每个单元格依次为 label, value, label, value...）
     * @param row         表格行
     * @param data        数据数组，从 0 开始，偶数索引为标签，奇数索引为值
     * @param colWidths   设置单元格列宽数组（与列数对应，null 表示不设置）
     */
    private void fillCustomerRow(XWPFTableRow row, String[] data, Integer[] colWidths) {

        row.setHeight(550);                                    // 设置单元格行高为 550
        List<XWPFTableCell> cells = row.getTableCells();       // 获取当前行的所有单元格

        // 循环填充单元格内容
        for (int i = 0; i < cells.size(); i++) {
            XWPFTableCell cell = cells.get(i);                  // 获取第 i 个单元格（从 0 开始）
            String text = data[i];                              // 获取单元格文本内容
            boolean isLabel = (i % 2 == 0);                     // 偶数索引为标签，灰色背景

            // 设置单元格文本内容及样式
            setCellText(cell, text, 9, ParagraphAlignment.CENTER, isLabel ? "f0f0f0" : null);
            // 设置单元格列宽
            if (colWidths != null && colWidths[i] != null) {
                setCellWidth(cell, colWidths[i]);
            }
        }
    }

    /**
     * 填充合并行的内容（第一列为标签，第二列及以后合并，显示值）
     * @param row         表格行
     * @param label       标签文字
     * @param value       值文字
     * @param mergeCols   需要合并的后续列数
     */
    private void fillMergedRow(XWPFTableRow row, String label, String value, int mergeCols) {
        List<XWPFTableCell> cells = row.getTableCells();

        // 1. 第一列：标签，设置单元格格式和文本内容
        XWPFTableCell cell0 = cells.get(0);
        setCellText(cell0, label, 9, ParagraphAlignment.CENTER, "f0f0f0");

        // 2. 第二列：开始合并，显示值
        XWPFTableCell cell1 = cells.get(1);

        // 设置本行单元格横向合并起点，.addNewHMerge() 表示横向合并
        cell1.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
        // 设置单元格格式和文本内容
        setCellText(cell1, value, 9, ParagraphAlignment.LEFT, null);
        // 设置左缩进为 200
        cell1.getParagraphArray(0).setIndentationLeft(200);

        // mergeCols 为需要合并的单元格数量，从 2 开始是因为这 mergeCols 个里的第一个设置为了 RESTART，将其余的单元格都设置为 STMerge.CONTINUE，表示继续合并，不显示这些单元格
        for (int i = 2; i <= mergeCols; i++) {
            cells.get(i).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
        }
    }

    /**
     * 创建体检项目表格，展示所有体检项目
     * @param doc               word 文档对象
     * @param checkupList       体检项目列表
     */
    private void createCheckup(XWPFDocument doc, List<CheckupItemDTO> checkupList) {
        // 1. 表格标题
        XWPFParagraph para = createParagraph(doc, "体检内容", 14, false, ParagraphAlignment.LEFT);
        setParagraphSpacing(para, 200, 100, null);

        // 2. 创建表格，宽度设置为 9850，行数 = 项目数 + 1（表头行），3列：序号、体检科室、体检内容
        XWPFTable table = doc.createTable(checkupList.size() + 1, 3);
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        tblPr.getTblW().setType(STTblWidth.DXA);
        tblPr.getTblW().setW(BigInteger.valueOf(9850));

        // 3. 设置表头行标签
        String[] headers = {"序号", "体检科室", "体检内容"};
        XWPFTableRow headerRow = table.getRow(0);
        headerRow.setHeight(550);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            // 填充单元格文本内容，设置单元格格式
            setCellText(cell, headers[i], 9, ParagraphAlignment.CENTER, "f0f0f0");
        }

        // 4. 填充数据（垂直合并相同科室）
        String lastPlace = null;                            // 记录上一个科室，用于判断是否需要合并单元格
        int index = 0;                                      // 科室计数，记录已经填充了几个科室

        for (int i = 0; i < checkupList.size(); i++) {
            // 获取第 i 个体检项
            CheckupItemDTO item = checkupList.get(i);
            XWPFTableRow row = table.getRow(i + 1);         // 获取当前行（+1 是因为前面表头行）
            row.setHeight(550);

            boolean samePlace = item.getPlace().equals(lastPlace);                      // 如果与前面一行是同一科室，则合并，否则设置为合并单元格的开头
            STMerge.Enum vMerge = samePlace ? STMerge.CONTINUE : STMerge.RESTART;       // 合并单元格的值，CONTINUE 表示继续合并，RESTART 表示重新开始合并
            if (!samePlace) {
                index++;
                lastPlace = item.getPlace();                                            // 设置 lastplace 为当前 place，为下一次合并单元格的开头
            }

            // 第一列：序号（垂直合并）
            XWPFTableCell cell0 = row.getCell(0);                                   // 获取当前行第一个单元格
            // .addNewVMerge() 表示纵向合并，RESTART 为开始合并的单元格，CONTINUE 为继续合并的单元格
            cell0.getCTTc().addNewTcPr().addNewVMerge().setVal(vMerge);
            // 设置单元格格式和文本内容
            setCellText(cell0, String.valueOf(index), 9, ParagraphAlignment.CENTER, null);

            // 第二列：科室（垂直合并）
            XWPFTableCell cell1 = row.getCell(1);
            cell1.getCTTc().addNewTcPr().addNewVMerge().setVal(vMerge);
            setCellText(cell1, item.getPlace(), 9, ParagraphAlignment.CENTER, null);

            // 第三列：项目名称（不合并）
            XWPFTableCell cell2 = row.getCell(2);
            setCellText(cell2, item.getItem(), 9, ParagraphAlignment.CENTER, null);
        }
    }

    /**
     * 使用 template1 的体检结果表格
     * @param doc           word 文档对象
     * @param resultDto     体检结果数据
     */
    private void createCheckupResultByTemplate1(XWPFDocument doc, CheckupResultDTO resultDto) {
        // 1. 科室标题
        XWPFParagraph para = createParagraph(doc, "【" + resultDto.getPlace() + "体检结果】", 14, false, ParagraphAlignment.LEFT);
        para.setBorderBottom(Borders.BABY_RATTLE);
        // 2. 设置高亮背景（浅灰）
        para.getRuns().get(0).setTextHighlightColor("lightGray");
        setParagraphSpacing(para, 250, 150, null);

        // 3. 创建表格：表头 + 结果行数
        List<ResultItemDTO> items = resultDto.getResultItems();
        // 表头定义：文字#宽度
        String[] headerDefs = {"序号#800", "检查项目#2200", "检查结果#3000", "单位#1200", "参考值#1300"};
        // 创建表格，设置表头行标签文本和单元格列宽
        XWPFTable table = createTemplateTable(doc,items.size() + 1, 5, headerDefs);

        // 4. 填充结果行
        for (int i = 0; i < items.size(); i++) {
            ResultItemDTO item = items.get(i);                      // 获取第 i 个体检结果
            XWPFTableRow row = table.getRow(i + 1);            // 获取当前行（+1 是因为前面表头行）
            row.setHeight(500);

            // 当前行数据
            String[] rowData = {
                    String.valueOf(i + 1),                                          // 序号
                    item.getCheckupName(),                                             // 体检项目名称
                    item.getValue(),                                                   // 体检结果
                    item.getUnit() == null ? "" : item.getUnit(),                      // 数值单位
                    item.getStandard() == null ? "" : item.getStandard()               // 参考值
            };

            // 填充单元格数据
            for (int j = 0; j < rowData.length; j++) {
                XWPFTableCell cell = row.getCell(j);
                // 第三列单元格（检查结果列）左对齐，其余单元格居中对齐
                ParagraphAlignment align = (j == 2) ? ParagraphAlignment.LEFT : ParagraphAlignment.CENTER;
                // 设置单元格格式和文本内容
                setCellText(cell, rowData[j], 9, align, null);
                if (j == 2) {
                    // 若为第三列单元格，左缩进 200
                    cell.getParagraphArray(0).setIndentationLeft(200);
                }
            }
        }

        // 5. 体检医生和日期
        para = createParagraph(doc, "体检医生：" + resultDto.getDoctorName() + "\t\t\t\t\t\t\t\t\t日期：" + resultDto.getDate(),
                9, false, ParagraphAlignment.LEFT);
        setParagraphSpacing(para, 100, 150, null);
    }

    /**
     * 使用 template2 的体检结果表格
     * @param doc                   word 文档对象
     * @param resultDto             当前科室的体检结果
     */
    private void createCheckupResultByTemplate2(XWPFDocument doc, CheckupResultDTO resultDto) throws Exception {
        // 1. 科室标题
        XWPFParagraph para = createParagraph(doc, "【" + resultDto.getPlace() + "体检结果】", 14, false, ParagraphAlignment.LEFT);
        para.setBorderBottom(Borders.BABY_RATTLE);                      // 设置标题下划线
        para.getRuns().get(0).setTextHighlightColor("lightGray");       // 设置高亮背景（浅灰）
        setParagraphSpacing(para, 250, 150, null);     // 设置段落间距

        // 2. 插入图片（如果有）
        String imageUrl = resultDto.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
//            BufferedImage image = ImageIO.read(new URL(imageUrl));          // 根据 imageUrl 创建 URL 对象，并通过 ImageIO 读取网络图片，返回 BufferedImage 对象

            // 2.1 读取图片到本地
            // 创建 URL 对象，代表一个网络资源的地址，标识该网络资源
            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);                   // 连接超时 5 秒
            conn.setReadTimeout(10000);                     // 读取超时 10 秒

            // 2.2 通过调用 conn.getInputStream() 打开网络连接读取远程服务器返回的数据（即图片的二进制内容），返回一个输入流，try 中调用会在 try 结束后自动关闭输入流
            try (InputStream in = conn.getInputStream()) {
                // 将输入流中的所有数据一次性读取到内存中的字节数组，后续操作不再依赖网络
                // in 读取了网络流中的所有字节，并将其存入 imageBytes 数组。此时网络流 in 已经被消耗完毕（到达流的末尾），并且当离开 try 块时，in 会被自动关闭
                byte[] imageBytes = IOUtils.toByteArray(in);

                // 使用原始字节数据字节数组创建一个新的内存输入流
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                    // 解析输入流中的图片数据（根据文件头识别格式），并返回一个 BufferedImage 对象，其中包含图片的像素信息和尺寸
                    BufferedImage originalImage = ImageIO.read(bais);

                    // ImageIO.read() 无法识别图片格式时（例如 URL 指向的不是一个有效的图片文件）会返回 null，抛出异常避免后续空指针错误
                    if (originalImage == null) {
                        throw new HisException("无法解析图片格式，URL: " + imageUrl);
                    }

                    int originalWidth = originalImage.getWidth();
                    int originalHeight = originalImage.getHeight();

                    // 2.3 计算缩放后的显示尺寸
                    double scaling = 1.0;                                   // 初始化缩放比例为 1（不缩放）
                    if (originalWidth > 72 * 9.13) {                        // 限制插入的图片宽度最大不能超过 72 * 9.13（大约为 657 像素）,A4 纸宽度约 9.13 英寸，72 dpi 下约 657 像素
                        scaling = (72.0 * 9.13) / originalWidth;            // 根据原来的宽度，计算缩放比例，缩放比例 = 最大允许宽度 / 原始宽度，确保缩放后宽度不超过限制
                    }
                    int scaledWidth = (int) (originalWidth * scaling);      // 图片缩放后的宽高
                    int scaledHeight = (int) (originalHeight * scaling);

                    // 2.4 根据 URL 扩展名确定 POI 图片类型
                    int pictureType = detectPictureTypeFromUrl(imageUrl);

                    // 2.5 使用原始字节数据再创建一个输入流，用于插入图片（基于代码清晰性和避免潜在风险的考虑，每个流只完成一个任务，不重复使用其他流）
                    // 网络流（InputStream）只能被读取一次，而且读取后不能重置，内存流 ByteArrayInputStream 可以重复读
                    // 网络流是指通过 URL.openStream()、Socket.getInputStream() 等网络相关 API 获取的流
                    try (InputStream picStream = new ByteArrayInputStream(imageBytes)) {
                        insertImage(doc, picStream, ParagraphAlignment.LEFT, 100, scaledWidth, scaledHeight, pictureType);
                    }
                }
            } catch (IOException e) {  // Added: 捕获 IO 异常
                log.error("下载体检结果图片失败 URL={}", imageUrl, e);
                throw new HisException("下载体检结果图片失败", e);
            }
        }

        // 3. 创建表格（结构与模板 1 类似，但第三列支持换行）
        // 获取检查结果数据列表
        List<ResultItemDTO> items = resultDto.getResultItems();
        // 表头行数据和列宽
        String[] headerDefs = {"序号#800", "检查项目#2200", "检查结果#3000", "单位#1200", "参考值#1300"};
        // 创建表格，设置表头行标签文本和单元格列宽
        XWPFTable table = createTemplateTable(doc, items.size() + 1, 5, headerDefs);

        // 4. 填充结果行，第三列可能包含换行符（用"#"分隔）
        for (int i = 0; i < items.size(); i++) {
            ResultItemDTO item = items.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            row.setHeight(500);

            // 第一列：序号
            setCellText(row.getCell(0), String.valueOf(i + 1), 9, ParagraphAlignment.CENTER, null);
            // 第二列：检查项目名称
            setCellText(row.getCell(1), item.getCheckupName(), 9, ParagraphAlignment.CENTER, null);
            // 第三列：检查结果（可能多行）
            XWPFTableCell cellResult = row.getCell(2);
            setCellText(cellResult, "", 9, ParagraphAlignment.LEFT, null);      // 设置该单元格的格式和文本内容（文本内容暂时为空字符串）
            cellResult.removeParagraph(0);                                                      // 先清空默认段落，后面手动添加
            String[] lines = item.getValue().split("#");                                       // 将用 # 连接的多个检查结果分割开

            // 多个检查结果时，填充到当前单元格的不同段落中
            for (String line : lines) {
                XWPFParagraph p = cellResult.addParagraph();            // 给单元格添加段落
                p.setAlignment(ParagraphAlignment.LEFT);                // 单元格内容左对齐
                p.setIndentationLeft(200);                              // 左缩进 200
                XWPFRun run = p.createRun();                            // 创建文本运行对象，设置段落文本内容
                run.setFontSize(9);
                run.setFontFamily("Microsoft YaHei");
                run.setText(line);
            }
            // 第四列：单位
            setCellText(row.getCell(3), item.getUnit() == null ? "" : item.getUnit(),
                    9, ParagraphAlignment.CENTER, null);
            // 第五列：参考值
            setCellText(row.getCell(4), item.getStandard() == null ? "" : item.getStandard(),
                    9, ParagraphAlignment.CENTER, null);
        }

        // 表格底部：体检医生和日期
        para = createParagraph(doc, "体检医生：" + resultDto.getDoctorName() + "\t\t\t\t\t\t\t\t\t日期：" + resultDto.getDate(),
                9, false, ParagraphAlignment.LEFT);
        setParagraphSpacing(para, 100, 150, null);
    }

    /**
     * 在文档末尾创建新段落，并设置段落的基本格式和文本内容
     * @param doc               文档对象
     * @param text              段落文本内容
     * @param fontSize          文本字体大小，POI 中 setFontSize(int size) 的单位是“磅”（1pt = 1/72 英寸）
     * @param bold              是否加粗
     * @param alignment         段落对齐方式
     * @return
     */
    private XWPFParagraph createParagraph(XWPFDocument doc, String text, int fontSize,
                                          boolean bold, ParagraphAlignment alignment) {
        XWPFParagraph paragraph = doc.createParagraph();            // createParagraph() 在文档末尾追加一个新段落对象
        paragraph.setAlignment(alignment);                          // 设置段落对齐方式
        XWPFRun run = paragraph.createRun();                        // 创建文本运行对象，XWPFRun 表示一段具有相同格式（字体、颜色、大小等）的连续文本
        run.setFontFamily("Microsoft YaHei");                       // 设置文本字体
        run.setFontSize(fontSize);
        run.setBold(bold);                                          // 是否加粗
        run.setText(text);                                          // 设置文本内容，如果同一个对象多次调用 setText 会覆盖之前的文本内容
        return paragraph;
    }

    /**
     * 设置段落间距（段前、段后、行距）
     * @param before  段前距，null表示不设置
     * @param after   段后距，null表示不设置
     * @param line    段落行距，null表示不设置（固定值）
     */
    private void setParagraphSpacing(XWPFParagraph paragraph, Integer before, Integer after, Integer line) {
        // 获取段落底层的 XML 对象（CTP = Complex Type Paragraph），通过该对象操作段落属性
        CTP ctp = paragraph.getCTP();
        // 判断段落是否已经有段落属性对象 pPr，如果有则获取（getPPr），没有则创建（addNewPPr）
        // 重复调用 addNewPPr()，会新增多个 pPr 节点，可能只会读取最后一个或只读取第一个 pPr，导致某些格式属性设置失效，所以必须判断
        CTPPr ctpPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        // 判断是否有段落行距对象 spacing，如果有则获取（getSpacing），没有则创建（addNewSpacing）
        // 重复调用 addNewSpacing()，会新增多个 spacing 节点，一般只会读取最后一个，显示效果相同，但是不规范，在生产代码中需要避免
        CTSpacing spacing = ctpPr.isSetSpacing() ? ctpPr.getSpacing() : ctpPr.addNewSpacing();

        // 是否传入段前段后间距数值，若没有则不设置
        if (before != null) {
            spacing.setBefore(BigInteger.valueOf(before));
        }
        if (after != null) {
            spacing.setAfter(BigInteger.valueOf(after));
        }
        // 设置段落行距
        if (line != null) {
            spacing.setLineRule(STLineSpacingRule.EXACT);       // 固定行距
            spacing.setLine(BigInteger.valueOf(line));

        }
    }

    /**
     * 创建检查结果表格，并设置检查结果表格中的表头行
     * @param doc           word 文档对象
     * @param rows          表格行数
     * @param cols          表格列数
     * @param headerDefs    表头行数据
     * @return              创建的表格
     */
    private XWPFTable createTemplateTable(XWPFDocument doc, int rows, int cols, String[] headerDefs) {
        // 1. 创建表格：表头 + 结果行数
        XWPFTable table = doc.createTable(rows, cols);
        CTTblPr tblPr = table.getCTTbl().getTblPr();                    // 获取表格 XML 对象，再获取表格属性对象
        tblPr.getTblW().setType(STTblWidth.DXA);                        // 设置宽度单位为 DXA（twips，1/20 磅）
        tblPr.getTblW().setW(BigInteger.valueOf(9850));                 // 表格总宽度设置为 9850 twips（约等于页面可用宽度）

        XWPFTableRow headerRow = table.getRow(0);                   // 获取表格第一行对象（表头行），设置行高为 500
        headerRow.setHeight(500);
        for (int i = 0; i < headerDefs.length; i++) {

            String[] parts = headerDefs[i].split("#");              // 表头定义格式：列名#列宽，按 # 分割，、
            if (parts.length < 2) {
                throw new HisException("表头配置格式错误，应为 '名称#宽度'，实际为: " + headerDefs[i]);
            }
            String label = parts[0];
            int width = Integer.parseInt(parts[1]);                        // 单元格宽度

            XWPFTableCell cell = headerRow.getCell(i);                     // 获取第 i 个单元格

            // 设置单元格内容、格式和单元格宽度
            setCellText(cell, label, 9, i == 2 ? ParagraphAlignment.LEFT : ParagraphAlignment.CENTER, "f0f0f0");
            setCellWidth(cell, width);

            // 若为第三列单元格，设置为左对齐且左缩进 200
            if (i == 2) {
                cell.getParagraphArray(0).setIndentationLeft(200); // 第三列左缩进
            }
        }

        return table;
    }


    /**
     * 设置单元格文本内容及样式
     * @param cell          单元格
     * @param text          文本
     * @param fontSize      字体大小
     * @param alignment     对齐方式
     * @param bgColor       背景色（如 "f0f0f0"），null表示无背景
     */
    private void setCellText(XWPFTableCell cell, String text, int fontSize,
                             ParagraphAlignment alignment, String bgColor) {
        // 清空单元格原有段落（通常只有一个）
        for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }

        if (bgColor != null) {
            cell.setColor(bgColor);                         // 设置单元格背景色
        }

        XWPFParagraph para = cell.addParagraph();           // 向单元格中新建段落
        para.setAlignment(alignment);                       // 设置段落对齐方式
        XWPFRun run = para.createRun();                     // 为该段落创建文本运行对象
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(fontSize);
        run.setText(text);                                  // 设置单元格文本内容

        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);      // 单元格内容为垂直居中
    }

    /**
     * 设置单元格宽度
     * @param cell      单元格对象
     * @param width     单元格宽度
     */
    private void setCellWidth(XWPFTableCell cell, int width) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth tblWidth = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tblWidth.setW(BigInteger.valueOf(width));
    }


    /**
     * 将图片输入流插入到 Word 文档的一个新段落中（增强版，可指定图片类型）
     * @param doc           Word 文档对象
     * @param in            图片的输入流（包含原始图片数据）
     * @param align         段落对齐方式
     * @param afterSpacing  段后间距，可为 null
     * @param width         图片显示宽度（像素）
     * @param height        图片显示高度（像素）
     * @param pictureType   图片类型（使用 POI 的 Document.PICTURE_TYPE_* 常量，如 JPEG、PNG 等）
     * @throws Exception    图片插入过程中可能抛出的异常
     */
    private void insertImage(XWPFDocument doc, InputStream in, ParagraphAlignment align,
                             Integer afterSpacing, int width, int height, int pictureType) throws Exception {
        // 1. 创建新段落，用于放置图片
        XWPFParagraph para = doc.createParagraph();
        para.setAlignment(align);

        // 2. 设置段后间距（如果需要）
        if (afterSpacing != null) {
            setParagraphSpacing(para, null, afterSpacing, null);
        }

        // 3. 在段落中创建 Run，添加图片
        XWPFRun run = para.createRun();
        // 参数说明：图片输入流、图片类型、图片ID（空字符串）、宽度（EMU单位）、高度（EMU单位）
        // 注意：in 输入流会在 addPicture 内部被读取并关闭，无需手动关闭
        run.addPicture(in, pictureType, "", Units.pixelToEMU(width), Units.pixelToEMU(height));
    }

    /**
     * 根据图片 URL 推断图片类型
     * @param urlString     图片 URL
     * @return POI          图片类型常量，默认返回 Document.PICTURE_TYPE_JPEG
     */
    private int detectPictureTypeFromUrl(String urlString) {
        try {
            String path = new URL(urlString).getPath();
            // 去除查询参数
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf('?'));
            }
            path = path.toLowerCase();
            if (path.endsWith(".png")) {
                return Document.PICTURE_TYPE_PNG;
            } else if (path.endsWith(".gif")) {
                return Document.PICTURE_TYPE_GIF;
            } else if (path.endsWith(".bmp")) {
                return Document.PICTURE_TYPE_BMP;
            } else if (path.endsWith(".wmf")) {
                return Document.PICTURE_TYPE_WMF;
            } else if (path.endsWith(".emf")) {
                return Document.PICTURE_TYPE_EMF;
            } else {
                // 默认 JPEG
                return Document.PICTURE_TYPE_JPEG;               // 默认 JPEG（含 .jpg, .jpeg 等）
            }
        } catch (MalformedURLException e) {
            return Document.PICTURE_TYPE_JPEG;                   // 如果 URL 无效，返回默认
        }
    }
}
