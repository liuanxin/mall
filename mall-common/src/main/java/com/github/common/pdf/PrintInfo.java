package com.github.common.pdf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PrintInfo {

    /**
     * 宽, 默认是 A4(595)
     *
     * @see com.itextpdf.text.PageSize
     */
    private Float width;
    /**
     * 高, 默认是 A4(842)
     *
     * @see com.itextpdf.text.PageSize
     */
    private Float height;

    /** x 轴的整体偏移量, 正数则整体向右偏移, 负数则整体向左偏移 */
    private Float offsetX;
    /** y 轴的整体偏移量, 正数则整体向下偏移, 负数则整体向上偏移 */
    private Float offsetY;

    /** 水印信息 */
    private WatermarkInfo watermark;

    /** 需要做分页的表格头, 一个模板只能有一个分页 */
    private TableDynamicHead dynamicHead;
    /** 需要做分页的表格内容, 一个模板只能有一个分页 */
    private List<TableContent> dynamicContent;

    /** 占位内容 */
    private List<DataContent> contentInfo;
    /** 表格占位内容 */
    private List<TableInfo> tableInfo;

    public boolean hasWatermark() {
        return watermark != null && watermark.getValue() != null && !watermark.getValue().isEmpty();
    }
    public boolean hasSize() {
        return width != null && width > 0 && height != null && height > 0;
    }
    public List<?> pageList(Map<String, Object> data) {
        if (dynamicHead == null || dynamicContent == null || dynamicContent.isEmpty() || dynamicHead.notDraw()) {
            return null;
        }
        int headSize = dynamicHead.getFieldWidthList().size();
        int contentSize = dynamicContent.size();
        if (headSize != contentSize) {
            if (log.isErrorEnabled()) {
                log.error("表头的长度({})与内容个数({})必须一致", headSize, contentSize);
            }
            return null;
        }
        Object obj = data.get(dynamicHead.getFieldName());
        if (!(obj instanceof List)) {
            return null;
        }

        List<?> list = (List<?>) obj;
        return list.isEmpty() ? null : list;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class WatermarkInfo {

        /** 水印内容 */
        private String value;
        /** 水印字体大小, 不设置则默认是 60 */
        private Float fontSize;
        /** 水印的 rgba 值, alpha 可以忽略, 全部不设置则默认是 [240,240,240,120] */
        private List<Integer> rgba;
        /** 第一个水印的 x 轴, 不设置则默认是 256 */
        private Float x;
        /** 第一个水印的 y 轴, 不设置则默认是 215 */
        private Float y;
        /** 水印旋转的度数, 不设置则默认是 45 */
        private Float rotation;
        /** 水印在每一页的个数, 不设置则默认是 3 */
        private Integer count;
        /** 多个水印的间距, 不设置则默认是 200 */
        private Integer spacing;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class DataContent {
        /** x 轴表示从左向右, 写的左下的位置 */
        private Float x;
        /** y 轴表示从下到上, 写的左下的位置, 要注意写的内容的高 */
        private Float y;

        /** 内容, 会附加上模板的值 */
        private String value;
        /** 字段类型, 不设置则默认是 STRING */
        private PlaceholderType fieldType;
        /** 模板对应的属性名 */
        private String fieldName;
        /** 拼在内容最后的值 */
        private String valueSuffix;
        /** 中文字体里的字母和英文看起来会显得很紧凑, 是否需要加一个空格, 不设置则默认是 false */
        private Boolean space;

        /** 字体大小, 单位 pt(11pt = 14.67px, 15pt = 20px, 20pt = 26.67px), 不设置则默认是 10 */
        private Float fontSize;
        /** 字体颜色 rgba 值 */
        private List<Integer> rgba;
        /** 文字对齐, 不设置则默认是 Element.ALIGN_LEFT */
        private Integer textAlign;
        /** 文字粗体, 不设置则默认是 false */
        private Boolean fontBold;

        /** 二维码(默认是 80)、条形码(默认是 160) 的宽 */
        private Float codeWidth;
        /** 二维码(默认是 80)、条形码(默认是 45) 的高 */
        private Float codeHeight;
        /** 条形码 的字体大小, 默认是 10 */
        private Float barCodeTextSize;
        /** 条形码文字跟码的距离, 大于 0 表示文字(在下文)跟码的距离; 小于等于 0 表示文字(在上方)跟码的距离. 默认是 10 */
        private Float barCodeBaseLine;

        /** 线的轨迹: [ [x1, y1] , [x2, y2] ... ] */
        private List<List<Float>> lineTrack;
        /** 线宽, 大于 0 才会起作用, 不设置则默认是 0 */
        private Float lineWidth;
        /** 线的底色: 0.黑, 1.白, 大于 0 才会起作用, 不设置则默认是 0 */
        private Float lineGray;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class TableHead {
        /** x 轴表示从左向右, 写的左下的位置 */
        private float x;
        /** y 轴表示从下到上, 写的左下的位置, 要注意写的内容的高 */
        private float y;

        /** 模板对应的属性名 */
        private String fieldName;
        /** 表格列宽(每个列的宽) */
        private List<Float> fieldWidthList;

        /** 是否输出表头, 不设置则默认是 true */
        private Boolean printHead;
        /** 表格头名(长度要跟列宽一致) */
        private List<String> headList;
        /** 中文字体里的字母和英文看起来会显得很紧凑, 是否需要加一个空格, 不设置则默认是 false */
        private Boolean space;
        /** 表头字体大小, 单位 pt(11pt = 14.67px, 15pt = 20px, 20pt = 26.67px), 不设置则默认是 10 */
        private Float fontSize;
        /** 字体颜色 rgba 值 */
        private List<Integer> rgba;
        /** 表头的背景 rgba 值 */
        private List<Integer> backRgba;
        /** 文字对齐, 不设置则默认是 Element.ALIGN_LEFT */
        private Integer textAlign;
        /** 文字粗体, 不设置则默认是 false */
        private Boolean fontBold;
        /** 表格头的高, 单位 pt(11pt = 14.67px, 15pt = 20px, 20pt = 26.67px), 不设置则默认是 15 */
        private Integer headHeight;
        /** 表格行的高, 单位 pt(11pt = 14.67px, 15pt = 20px, 20pt = 26.67px), 不设置则默认是 20 */
        private Integer contentHeight;
        /** 表格是否有边框, 不设置则默认是 false */
        private Boolean border;

        public boolean notDraw() {
            return x < 0 || y < 0 || fieldName == null || "".equals(fieldName.trim())
                    || fieldWidthList == null || fieldWidthList.isEmpty()
                    || (printHead != null && printHead && fieldWidthList.size() != headList.size());
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class TableDynamicHead extends TableHead {
        /** 单页最多存放个数, 不设置则默认是 10 */
        private Integer singlePageCount;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class TableContent {
        /** 模板对应的属性名 */
        private String fieldName;
        /** 拼在内容最后的值 */
        private String valueSuffix;
        /** 中文字体里的字母和英文看起来会显得很紧凑, 是否需要加一个空格, 不设置则默认是 false */
        private Boolean space;

        /** 字段类型, 不设置则默认是 STRING */
        private PlaceholderType fieldType;
        /** 字体大小, 单位 pt(11pt = 14.67px, 15pt = 20px, 20pt = 26.67px), 不设置则默认是 10 */
        private Float fontSize;
        /** 字体颜色 rgba 值 */
        private List<Integer> rgba;
        /** 文字对齐, 不设置则默认是 Element.ALIGN_LEFT */
        private Integer textAlign;
        /** 文字粗体, 不设置则默认是 false */
        private Boolean fontBold;
        /** 如果内容过大, 只显示多少个字符. 超出字符会使用 ... 显示, 大于 0 才会起作用, 不设置则默认是 0 */
        private Integer maxCount;

        /** 二维码(默认是 80)、条形码(默认是 160) 的宽 */
        private Float codeWidth;
        /** 二维码(默认是 80)、条形码(默认是 45) 的高 */
        private Float codeHeight;
        /** 条形码 的字体大小, 设置为 0 表示不显示文字, 不设置则默认是 10 */
        private Float barCodeTextSize;
        /** 条形码文字跟码的距离, 大于 0 表示文字(在下文)跟码的距离; 小于等于 0 表示文字(在上方)跟码的距离. 默认是 10 */
        private Float barCodeBaseLine;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class TableInfo {
        private TableHead key;
        private List<TableContent> value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum PlaceholderType {
        STRING(1),
        BARCODE(2),
        QRCODE(3),
        LINE(4),
        /** 用在表上表示「所在数据位置」, 用在占位内容上表示「所在页数」, 数值从 1 开始 */
        INDEX(5),
        /** 用在表上表示「所在数据的总条数」, 用在占位内容上表示「总页数」 */
        COUNT(6),
        /** 用在表上表示「数据位置/数据的总条数」, 用在占位内容上表示「所在页数/总页数」, 使用 / 隔开 */
        INDEX_COUNT(7);

        @JsonValue
        private final int code;
        @JsonCreator
        public static PlaceholderType deserializer(Object obj) {
            if (obj != null) {
                String trim = obj.toString().trim();
                for (PlaceholderType value : values()) {
                    if (trim.equals(String.valueOf(value.code)) || trim.equals(value.name())) {
                        return value;
                    }
                }
            }
            return STRING;
        }
    }
}
