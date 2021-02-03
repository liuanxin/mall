package com.github.common.export.easy;

import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
@Slf4j
public class WidthAndStyleCellHandler implements CellWriteHandler {

    /** 标题行字体大小 */
    private static final short TITLE_FONT_SIZE = 14;
    /** 行字体大小 */
    private static final short FONT_SIZE = 12;

    private static final Cache<Thread, Map<String, CellStyle>> STYLE_CACHE =
            CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();


    @Override
    public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 Row row, Head head, Integer columnIndex, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        if (isHead != null) {
            if (isHead) {
                cell.setCellStyle(headStyle(cell.getSheet().getWorkbook()));
                settingWidth(cell, head.getHeadNameList().get(relativeRowIndex));
            }
        }
    }

    private void settingWidth(Cell cell, Object data) {
        Sheet sheet = cell.getSheet();
        int columnIndex = cell.getColumnIndex();
        int oldWidth = sheet.getColumnWidth(columnIndex);
        // 设置列宽: 从内容来确定, 中文为 2 个长度, 左移 8 相当于 * 256
        int currentWidth = (U.toLen(data) << 8);
        if (currentWidth > oldWidth) {
            sheet.setColumnWidth(columnIndex, currentWidth);
        }
    }

    @Override
    public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                       CellData cellData, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        if (isHead != null) {
            if (!isHead) {
                if (U.isNotNull(cellData)) {
                    CellDataTypeEnum dataType = cellData.getType();
                    if (dataType == CellDataTypeEnum.NUMBER || dataType == CellDataTypeEnum.BOOLEAN) {
                        cell.setCellStyle(numberStyle(cell.getSheet().getWorkbook()));
                    } else if (dataType == CellDataTypeEnum.STRING || dataType == CellDataTypeEnum.DIRECT_STRING) {
                        cell.setCellStyle(contentStyle(cell.getSheet().getWorkbook()));
                    }
                }
                settingWidth(cell, cellData.toString());
            }
        }
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List<CellData> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
    }


    /** 头样式: 垂直居中, 水平居左, 粗体, 字体大小 12 */
    private static CellStyle headStyle(Workbook workbook) {
        String key = "head";
        Thread currentThread = Thread.currentThread();
        Map<String, CellStyle> styleMap = STYLE_CACHE.getIfPresent(currentThread);
        if (A.isEmpty(styleMap)) {
            styleMap = Maps.newConcurrentMap();
        } else if (styleMap.containsKey(key)) {
            return styleMap.get(key);
        }

        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints(TITLE_FONT_SIZE);
        style.setFont(font);

        styleMap.put(key, style);
        STYLE_CACHE.put(currentThread, styleMap);
        return style;
    }

    /** 内容样式: 垂直居中, 水平居左, 字体大小 10 */
    private static CellStyle contentStyle(Workbook workbook) {
        String key = "content";
        Thread currentThread = Thread.currentThread();
        Map<String, CellStyle> styleMap = STYLE_CACHE.getIfPresent(currentThread);
        if (A.isEmpty(styleMap)) {
            styleMap = Maps.newConcurrentMap();
        } else if (styleMap.containsKey(key)) {
            return styleMap.get(key);
        }

        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);

        styleMap.put(key, style);
        STYLE_CACHE.put(currentThread, styleMap);
        return style;
    }

    /** 数字样式: 垂直居中, 水平居右, 字体大小 10 */
    private static CellStyle numberStyle(Workbook workbook) {
        String key = "number";
        Thread currentThread = Thread.currentThread();
        Map<String, CellStyle> styleMap = STYLE_CACHE.getIfPresent(currentThread);
        if (A.isEmpty(styleMap)) {
            styleMap = Maps.newConcurrentMap();
        } else if (styleMap.containsKey(key)) {
            return styleMap.get(key);
        }

        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.RIGHT);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);

        styleMap.put(key, style);
        STYLE_CACHE.put(currentThread, styleMap);
        return style;
    }
}
