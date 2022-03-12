package com.github.common.pdf;

import com.github.common.encrypt.Encrypt;
import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PdfUtil {

    private static final Pattern CN_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern SPACE_PATTERN = Pattern.compile("([a-zA-Z0-9])");
    private static final Map<EncodeHintType, Object> HINTS = new HashMap<>();
    static {
        HINTS.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    }

    /** 英文字体 */
    private static final BaseFont BASE_FONT = new FontFactoryImp().getFont(BaseFont.COURIER).getBaseFont();
    /** 英文字体 - 粗体 */
    private static final BaseFont BASE_FONT_BOLD = new FontFactoryImp().getFont(BaseFont.COURIER_BOLD).getBaseFont();

    /** 中文字体 */
    private static final BaseFont CHINESE_BASE_FONT;
    /** 中文字体 - 粗体 */
    private static final BaseFont CHINESE_BASE_FONT_BOLD;
    static {
        String cnFontName = "STSong-Light";
        String cnBoldFontName = cnFontName + ",Bold";
        String cnEncoding = "UniGB-UCS2-H";

        BaseFont baseCnFont;
        try {
            baseCnFont = BaseFont.createFont(cnFontName, cnEncoding, BaseFont.EMBEDDED);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("装载字体({})异常", cnFontName, e);
            }
            baseCnFont = BASE_FONT;
        }
        CHINESE_BASE_FONT = baseCnFont;

        BaseFont baseFontCnBold;
        try {
            baseFontCnBold = BaseFont.createFont(cnBoldFontName, cnEncoding, BaseFont.EMBEDDED);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("装载字体({})异常", cnBoldFontName, e);
            }
            baseFontCnBold = BASE_FONT_BOLD;
        }
        CHINESE_BASE_FONT_BOLD = baseFontCnBold;
    }

    private static final boolean USE_CACHE = false;
    private static final Cache<String, Object> CACHE = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();


    public static void generatePdfFile(String file, String template, Map<String, Object> data) {
        generatePdfFile(file, JsonUtil.toObject(template, PrintInfo.class), data);
    }
    public static void generatePdfFile(String file, PrintInfo template, Map<String, Object> data) {
        if (template != null) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("开始生成 pdf 文件({})", file);
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                long start = System.currentTimeMillis();
                writePdf(template, data, outputStream);
                if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                    LogUtil.ROOT_LOG.debug("生成 pdf 文件({})耗时({}ms)", file, (System.currentTimeMillis() - start));
                }
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("生成 pdf 文件({})异常", file, e);
                }
            }
        }
    }

    public static byte[] generatePdfByte(String template, Map<String, Object> data) {
        return generatePdfByte(JsonUtil.toObject(template, PrintInfo.class), data);
    }
    public static byte[] generatePdfByte(PrintInfo template, Map<String, Object> data) {
        if (template != null) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("开始生成 pdf 字节");
            }
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                long start = System.currentTimeMillis();
                writePdf(template, data, outputStream);
                if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                    LogUtil.ROOT_LOG.debug("生成 pdf 字节耗时({}ms)", (System.currentTimeMillis() - start));
                }
                return outputStream.toByteArray();
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("生成 pdf 字节异常", e);
                }
            }
        }
        return null;
    }

    private static void writePdf(PrintInfo template, Map<String, Object> data, OutputStream outputStream) throws DocumentException {
        Document document;
        if (template.hasSize()) {
            document = new Document(new Rectangle(template.getWidth(), template.getHeight()));
        } else {
            document = new Document();
        }

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        if (template.hasWatermark()) {
            writer.setPageEvent(new WatermarkEvent(template.getWatermark()));
        }
        document.open();
        PdfContentByte canvas = writer.getDirectContent();

        float offsetX = toFloat(template.getOffsetX(), 0);
        float offsetY = toFloat(template.getOffsetY(), 0);

        String dynamicContentKey = template.getDynamicContentKey();
        List<?> placeContent = placeContent(data, dynamicContentKey);

        List<?> list = template.pageList(data);
        if (list != null) {
            PrintInfo.TableDynamicHead tableHead = template.getDynamicHead();
            List<PrintInfo.TableContent> tableContent = template.getDynamicContent();
            if (tableHead != null && tableContent != null && tableContent.size() > 0) {

                int total = list.size();
                int pageCount = toInt(tableHead.getSinglePageCount(), 10);
                int loopCount = (total % pageCount == 0) ? total / pageCount : (total / pageCount) + 1;
                loopCount = Math.max(Math.max(loopCount, placeContent.size()), 1);

                for (int i = 0; i < loopCount; i++) {
                    int fromIndex = pageCount * i;
                    boolean notLastPage = (i + 1 != loopCount);
                    int toIndex = notLastPage ? (fromIndex + pageCount) : total;
                    draw(i, loopCount, data, template, offsetX, offsetY, canvas);
                    drawPlaceContent(template, canvas, offsetX, offsetY, placeContent, total, loopCount, i);

                    List<?> pageDataList = list.subList(fromIndex, toIndex);
                    drawDynamicTable(i * pageCount, total, pageDataList, tableHead, tableContent, offsetX, offsetY, canvas);
                    if (notLastPage) {
                        document.newPage();
                    }
                }
            }
        } else {
            int loopCount = placeContent.size();
            for (int i = 0; i < loopCount; i++) {
                drawPlaceContent(template, canvas, offsetX, offsetY, placeContent, loopCount, loopCount, i);
                draw(0, 1, data, template, offsetX, offsetY, canvas);
                if (i + 1 != loopCount) {
                    document.newPage();
                }
            }
        }

        document.add(new Chunk(""));
        document.close();
    }

    private static void drawPlaceContent(PrintInfo template, PdfContentByte canvas, float offsetX, float offsetY,
                                         List<?> placeContent, int total, int loopCount, int i) {
        List<PrintInfo.DataContent> dynamicContentList = template.getDynamicContentList();
        if (!placeContent.isEmpty() && placeContent.size() >= loopCount
                && dynamicContentList != null && !dynamicContentList.isEmpty()) {
            for (PrintInfo.DataContent dataContent : dynamicContentList) {
                Object obj = placeContent.get(i);
                if (obj instanceof Map) {
                    writeDataContent(i, total, canvas, offsetX, offsetY, dataContent, (Map<String, Object>) obj);
                }
            }
        }
    }

    private static List<?> placeContent(Map<String, Object> data, String dynamicContentKey) {
        Object dynamicContent = data.get(dynamicContentKey);
        List<?> placeContent;
        if ((dynamicContent instanceof List)) {
            placeContent = (List) dynamicContent;
        } else {
            if (dynamicContent != null) {
                if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                    LogUtil.ROOT_LOG.warn("动态占位数据有误");
                }
            }
            placeContent = Collections.emptyList();
        }
        return placeContent;
    }

    private static void draw(int page, int total, Map<String, Object> data, PrintInfo printInfo,
                             float offsetX, float offsetY, PdfContentByte canvas) {
        List<PrintInfo.DataContent> contentList = printInfo.getContentInfo();
        if (contentList != null && !contentList.isEmpty()) {
            for (PrintInfo.DataContent dataContent : contentList) {
                writeDataContent(page, total, canvas, offsetX, offsetY, dataContent, data);
            }
        }
        List<PrintInfo.TableInfo> tableInfoList = printInfo.getTableInfo();
        if (tableInfoList != null && !tableInfoList.isEmpty()) {
            for (PrintInfo.TableInfo tableInfo : tableInfoList) {
                PrintInfo.TableHead table = tableInfo.getKey();
                List<PrintInfo.TableContent> tableContentList = tableInfo.getValue();
                writeTable(canvas, offsetX, offsetY, table, tableContentList, data);
            }
        }
    }

    private static void drawDynamicTable(int lineStart, int totalDataSize, List<?> pageDataList,
                                         PrintInfo.TableDynamicHead tableHead,
                                         List<PrintInfo.TableContent> tableContentList,
                                         float offsetX, float offsetY, PdfContentByte canvas) {
        if (!pageDataList.isEmpty()) {
            PdfPTable table = setTableWidth(tableHead, "设置动态表头宽时异常");
            writeTableHead(table, tableHead);

            int contentHeight = toInt(tableHead.getContentHeight(), 20);
            int dataSize = pageDataList.size();
            for (int i = 0; i < dataSize; i++) {
                writeTableContent(canvas, tableHead, tableContentList, pageDataList, table, contentHeight, totalDataSize, i, lineStart);
            }
            table.writeSelectedRows(0, -1, (tableHead.getX() + offsetX), (tableHead.getY() + offsetY), canvas);
        }
    }

    private static void writeDataContent(int page, int len, PdfContentByte canvas, float offsetX, float offsetY,
                                         PrintInfo.DataContent dataContent, Map<String, Object> data) {
        String prefix = toStr(dataContent.getValuePrefix());
        String suffix = toStr(dataContent.getValueSuffix());
        String value = prefix + toStr(dataContent.getValue()) + toStr(data.get(toStr(dataContent.getFieldName()))) + suffix;
        value = handleSpace(value, toBoolean(dataContent.getSpace(), false));

        float x = toFloat(dataContent.getX(), 0) + offsetX;
        float y = toFloat(dataContent.getY(), 0) + offsetY;

        float fontSize = toFloat(dataContent.getFontSize(), 10);
        int textAlign = toInt(dataContent.getTextAlign(), Element.ALIGN_LEFT);
        boolean bold = toBoolean(dataContent.getFontBold(), false);
        PrintInfo.PlaceholderType fieldType = dataContent.getFieldType();
        if (fieldType != null) {
            switch (fieldType) {
                case LINE:
                    float lineGray = toFloat(dataContent.getLineGray(), 0);
                    float lineWidth = toFloat(dataContent.getLineWidth(), 0);
                    List<List<Float>> lineTrack = dataContent.getLineTrack();
                    if (lineTrack != null && !lineTrack.isEmpty()) {
                        if (lineWidth > 0) {
                            canvas.setLineWidth(lineWidth);
                        }
                        if (lineGray > 0) {
                            canvas.setGrayStroke(lineGray);
                        }
                        int size = lineTrack.size();
                        for (int i = 0; i < size; i++) {
                            List<Float> tracks = lineTrack.get(i);
                            List<Float> nextTracks = lineTrack.get(((i + 1) == size) ? 0 : (i + 1));
                            if (tracks != null && tracks.size() == 2 && nextTracks != null && nextTracks.size() == 2) {
                                canvas.moveTo(tracks.get(0), tracks.get(1));
                                canvas.lineTo(nextTracks.get(0), nextTracks.get(1));
                            }
                        }
                        canvas.stroke();
                    }
                    return;
                case BARCODE:
                    float textSize = toFloat(dataContent.getBarCodeTextSize(), 10);
                    float baseLine = toBaseLine(dataContent.getBarCodeBaseLine());
                    float codeWidth = toFloat(dataContent.getCodeWidth(), 160);
                    float codeHeight = toFloat(dataContent.getCodeHeight(), 45);
                    Image barCode = generateBarCode(canvas, textSize, baseLine, codeWidth, codeHeight, value);
                    if (barCode != null) {
                        try {
                            barCode.setAbsolutePosition(x, y);
                            canvas.addImage(barCode);
                        } catch (Exception e) {
                            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                                LogUtil.ROOT_LOG.error("写 barcode 异常", e);
                            }
                        }
                    }
                    return;
                case QRCODE:
                    float qrCodeWidth = toFloat(dataContent.getCodeWidth(), 80);
                    float qrCodeHeight = toFloat(dataContent.getCodeHeight(), 80);
                    Image qrCode = generateQrCode(qrCodeWidth, qrCodeHeight, value);
                    if (qrCode != null) {
                        try {
                            qrCode.setAbsolutePosition(x, y);
                            canvas.addImage(qrCode);
                        } catch (Exception e) {
                            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                                LogUtil.ROOT_LOG.error("写 qrcode 异常", e);
                            }
                        }
                    }
                    return;
                case INDEX:
                    value = prefix + (page + 1) + suffix;
                    break;
                case COUNT:
                    value = prefix + len + suffix;
                    break;
                case INDEX_COUNT:
                    value = prefix + (page + 1) + "/" + len + suffix;
                    break;
            }
        }

        if (!value.isEmpty()) {
            canvas.beginText();
            BaseColor color = toColor(dataContent.getRgba());
            if (color != null) {
                canvas.setColorFill(color);
            }
            canvas.setFontAndSize(toBaseFont(value, bold), fontSize);
            canvas.showTextAligned(textAlign, value, x, y, 0);
            if (color != null) {
                canvas.setColorFill(BaseColor.BLACK);
            }
            canvas.endText();
        }
    }

    private static PdfPTable setTableWidth(PrintInfo.TableHead tableHead, String errorMsg) {
        List<Float> fieldWidth = tableHead.getFieldWidthList();
        int tableColumnSize = fieldWidth.size();

        float[] totalWidth = new float[tableColumnSize];
        for (int i = 0; i < tableColumnSize; i++) {
            totalWidth[i] = fieldWidth.get(i);
        }
        PdfPTable table = new PdfPTable(tableColumnSize);
        try {
            table.setTotalWidth(totalWidth);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(errorMsg, e);
            }
        }
        return table;
    }

    private static void writeTable(PdfContentByte canvas, float offsetX, float offsetY,
                                   PrintInfo.TableHead tableHead,
                                   List<PrintInfo.TableContent> tableContentList,
                                   Map<String, Object> data) {
        if (tableHead.notDraw() || tableContentList == null || tableContentList.isEmpty()) {
            return;
        }
        Object list = data.get(tableHead.getFieldName());
        if (!(list instanceof List)) {
            return;
        }

        PdfPTable table = setTableWidth(tableHead, "设置表头宽时异常");
        writeTableHead(table, tableHead);

        int contentHeight = toInt(tableHead.getContentHeight(), 20);
        List<?> dataList = (List<?>) list;
        int size = dataList.size();
        for (int i = 0; i < size; i++) {
            writeTableContent(canvas, tableHead, tableContentList, dataList, table, contentHeight, size, i, 0);
        }
        table.writeSelectedRows(0, -1, (tableHead.getX() + offsetX), (tableHead.getY() + offsetY), canvas);
    }

    private static void writeTableHead(PdfPTable table, PrintInfo.TableHead head) {
        boolean printHead = toBoolean(head.getPrintHead(), true);
        if (printHead) {
            boolean bold = toBoolean(head.getFontBold(), false);
            List<String> headList = head.getHeadList();
            float fontSize = toFloat(head.getFontSize(), 10);
            Font headFont = toHeadFont(headHasCn(headList), bold, fontSize, head.getRgba());

            BaseColor background = toColor(head.getBackRgba());
            boolean space = toBoolean(head.getSpace(), false);
            int height = toInt(head.getHeadHeight(), 15);
            boolean border = toBoolean(head.getBorder(), false);
            int textAlign = toInt(head.getTextAlign(), Element.ALIGN_LEFT);

            for (String headInfo : headList) {
                PdfPCell cell = new PdfPCell(generateValue(handleSpace(toStr(headInfo), space), bold, headFont));
                cell.setFixedHeight(height);
                if (!border) {
                    cell.setBorder(PdfPCell.NO_BORDER);
                }
                if (background != null) {
                    cell.setBackgroundColor(background);
                }
                cell.setUseAscender(true);
                cell.setHorizontalAlignment(textAlign);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }
        }
    }

    private static void writeTableContent(PdfContentByte canvas, PrintInfo.TableHead head,
                                          List<PrintInfo.TableContent> tableContentList, List<?> list,
                                          PdfPTable table, int contentHeight, int size, int i, int lineStart) {
        Object obj = list.get(i);
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (PrintInfo.TableContent tableContent : tableContentList) {
                String prefix = toStr(tableContent.getValuePrefix());
                String suffix = toStr(tableContent.getValueSuffix());
                String value = prefix + toStr(map.get(toStr(tableContent.getFieldName()))) + suffix;
                value = handleSpace(value, toBoolean(tableContent.getSpace(), false));

                int maxCount = toInt(tableContent.getMaxCount(), 0);
                if (maxCount > 0 && value.length() > maxCount) {
                    value = value.substring(0, maxCount) + " ...";
                }

                PdfPCell cell = new PdfPCell();
                cell.setFixedHeight(contentHeight);
                if (!toBoolean(head.getBorder(), false)) {
                    cell.setBorder(PdfPCell.NO_BORDER);
                }
                cell.setUseAscender(true);
                cell.setHorizontalAlignment(toInt(tableContent.getTextAlign(), Element.ALIGN_LEFT));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                BaseColor color = toColor(tableContent.getRgba());
                PrintInfo.PlaceholderType fieldType = tableContent.getFieldType();
                float fontSize = toFloat(tableContent.getFontSize(), 10);
                boolean fontBold = toBoolean(tableContent.getFontBold(), false);
                if (fieldType != null) {
                    switch (fieldType) {
                        case BARCODE:
                            float textSize = toFloat(tableContent.getBarCodeTextSize(), 10);
                            float baseLine = toBaseLine(tableContent.getBarCodeBaseLine());
                            float codeWidth = toFloat(tableContent.getCodeWidth(), 160);
                            float codeHeight = toFloat(tableContent.getCodeHeight(), 45);
                            Image barCode = generateBarCode(canvas, textSize, baseLine, codeWidth, codeHeight, value);
                            if (barCode != null) {
                                cell.setImage(barCode);
                            }
                            break;
                        case QRCODE:
                            float qrCodeWidth = toFloat(tableContent.getCodeWidth(), 80);
                            float qrCodeHeight = toFloat(tableContent.getCodeHeight(), 80);
                            Image qrCode = generateQrCode(qrCodeWidth, qrCodeHeight, value);
                            if (qrCode != null) {
                                cell.setImage(qrCode);
                            }
                            break;
                        case INDEX:
                            String lineIndex = prefix + (lineStart + i + 1) + suffix;
                            cell.setPhrase(generateValue(toStr(lineIndex), fontBold, toFont(lineIndex, fontBold, fontSize, color)));
                            break;
                        case COUNT:
                            String lineCount = prefix + size + suffix;
                            cell.setPhrase(generateValue(toStr(lineCount), fontBold, toFont(lineCount, fontBold, fontSize, color)));
                            break;
                        case INDEX_COUNT:
                            String lineIndexCount = prefix + (lineStart + i + 1) + "/" + size + suffix;
                            cell.setPhrase(generateValue(toStr(lineIndexCount), fontBold, toFont(lineIndexCount, fontBold, fontSize, color)));
                            break;
                        default:
                            cell.setPhrase(generateValue(toStr(value), fontBold, toFont(value, fontBold, fontSize, color)));
                            break;
                    }
                } else {
                    cell.setPhrase(generateValue(toStr(value), fontBold, toFont(value, fontBold, fontSize, color)));
                }
                table.addCell(cell);
            }
        }
    }

    private static boolean headHasCn(List<String> headList) {
        for (String head : headList) {
            if (CN_PATTERN.matcher(head).find()) {
                return true;
            }
        }
        return false;
    }

    private static Font toHeadFont(boolean hasCn, boolean bold, float fontSize, List<Integer> rgba) {
        String cacheKey;
        if (USE_CACHE) {
            cacheKey = String.format("head-font-%s-%s-%s-%s", (hasCn ? 1 : 0), (bold ? 1 : 0), fontSize, rgba);
            Object valueInCache = CACHE.getIfPresent(cacheKey);
            if (valueInCache instanceof Font) {
                return (Font) valueInCache;
            }
        }

        BaseFont headBaseFont;
        if (hasCn) {
            headBaseFont = bold ? CHINESE_BASE_FONT_BOLD : CHINESE_BASE_FONT;
        } else {
            headBaseFont = bold ? BASE_FONT_BOLD : BASE_FONT;
        }
        Font headFont = new Font(headBaseFont, fontSize);
        BaseColor color = toColor(rgba);
        if (color != null) {
            headFont.setColor(color);
        }
        if (USE_CACHE) {
            CACHE.put(cacheKey, headFont);
        }
        return headFont;
    }

    private static Phrase generateValue(String value, boolean bold, Font font) {
        String cacheKey;
        if (USE_CACHE) {
            Font.FontFamily fontFamily = font.getFamily();
            int family = fontFamily != null ? fontFamily.ordinal() : 0;
            BaseColor baseColor = font.getColor();
            int color = baseColor != null ? baseColor.hashCode() : 0;
            float size = font.getSize();
            int style = font.getStyle();
            String v = (value.length() > 32) ? Encrypt.toMd5(value) : value;
            cacheKey = String.format("value-%s-%s-%s-%s-%s-%s", v, (bold ? 1 : 0), family, size, style, color);
            Object valueInCache = CACHE.getIfPresent(cacheKey);
            if (valueInCache instanceof Phrase) {
                return (Phrase) valueInCache;
            }
        }

        Phrase phrase = new Phrase(value, font);
        if (USE_CACHE) {
            CACHE.put(cacheKey, phrase);
        }
        return phrase;
    }

    private static Image generateBarCode(PdfContentByte canvas, float textSize, float baseLine,
                                         float width, float height, String value) {
        String cacheKey;
        if (USE_CACHE) {
            cacheKey = String.format("bar-%s-%s-%s-%s-%s", value, textSize, baseLine, width, height);
            Object valueInCache = CACHE.getIfPresent(cacheKey);
            if (valueInCache instanceof Image) {
                return (Image) valueInCache;
            }
        }

        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(value);
            if (textSize == 0) {
                barcode.setFont(null);
            }
            barcode.setSize(textSize);
            barcode.setBaseline(baseLine);
            Image image = barcode.createImageWithBarcode(canvas, null, null);
            image.scaleAbsolute(width, height);
            if (USE_CACHE) {
                CACHE.put(cacheKey, image);
            }
            return image;
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("生成 barCode 异常", e);
            }
            return null;
        }
    }

    private static Image generateQrCode(float width, float height, String value) {
        String cacheKey;
        if (USE_CACHE) {
            cacheKey = String.format("qr-%s-%s-%s", value, width, height);
            Object valueInCache = CACHE.getIfPresent(cacheKey);
            if (valueInCache instanceof Image) {
                return (Image) valueInCache;
            }
        }

        try {
            Image image = new BarcodeQRCode(value, (int) width, (int) height, HINTS).getImage();
            image.scaleAbsolute(width, height);
            if (USE_CACHE) {
                CACHE.put(cacheKey, image);
            }
            return image;
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("生成 qrCode 异常", e);
            }
            return null;
        }
    }

    private static BaseColor toColor(List<Integer> rgba) {
        if (rgba != null && !rgba.isEmpty() && rgba.size() >= 3) {
            int red = toColorInt(rgba.get(0));
            int green = toColorInt(rgba.get(1));
            int blue = toColorInt(rgba.get(2));

            if (red > 0 || green > 0 || blue > 0) {
                int alpha = (rgba.size() > 3) ? toColorInt(rgba.get(3)) : 0;
                String cacheKey;
                if (USE_CACHE) {
                    cacheKey = String.format("color-%s-%s-%s-%s", red, green, blue, alpha);
                    Object valueInCache = CACHE.getIfPresent(cacheKey);
                    if (valueInCache instanceof BaseColor) {
                        return (BaseColor) valueInCache;
                    }
                }

                BaseColor color = (alpha > 0) ? new BaseColor(red, green, blue, alpha) : new BaseColor(red, green, blue);
                if (USE_CACHE) {
                    CACHE.put(cacheKey, color);
                }
                return color;
            }
        }
        return null;
    }
    private static int toColorInt(Integer num) {
        return Math.min(toInt(num, 0), 255);
    }

    private static BaseFont toBaseFont(String value, boolean bold) {
        return toBaseFont(value != null && CN_PATTERN.matcher(value).find(), bold);
    }
    private static BaseFont toBaseFont(boolean hasCn, boolean bold) {
        if (hasCn) {
            return bold ? CHINESE_BASE_FONT_BOLD : CHINESE_BASE_FONT;
        } else {
            return bold ? BASE_FONT_BOLD : BASE_FONT;
        }
    }
    private static Font toFont(String value, boolean bold, float fontSize, BaseColor color) {
        boolean hasCn = value != null && CN_PATTERN.matcher(value).find();
        String cacheKey;
        if (USE_CACHE) {
            String c = ( (color == null) ? "" : ("-" + color.getRGB()) );
            cacheKey = String.format("font-%s-%s-%s%s", (hasCn ? 1 : 0), (bold ? 1 : 0), fontSize, c);
            Object valueInCache = CACHE.getIfPresent(cacheKey);
            if (valueInCache instanceof Font) {
                return (Font) valueInCache;
            }
        }

        BaseFont baseFont = toBaseFont(hasCn, bold);
        Font font = new Font(baseFont, fontSize, (bold ? Font.BOLD : Font.UNDEFINED), color);
        if (USE_CACHE) {
            CACHE.put(cacheKey, font);
        }
        return font;
    }

    private static float toBaseLine(Float num) {
        return num == null ? (float) 10 : num;
    }
    private static String toStr(Object obj) {
        return (obj == null) ? "" : obj.toString();
    }
    private static String handleSpace(String str, boolean space) {
        // 中文字体里的字母和英文看起来会显得很紧凑, 加一个空格
        if (space && CN_PATTERN.matcher(str).find()) {
            return SPACE_PATTERN.matcher(str).replaceAll(" $1 ").replace("  ", " ");
        } else {
            return str;
        }
    }
    private static float toFloat(Float num, float defaultValue) {
        return num == null || num < 0 ? defaultValue : num;
    }
    public static int toInt(Integer num, int defaultValue) {
        return num == null || num < 0 ? defaultValue : num;
    }
    public static boolean toBoolean(Boolean bool, boolean defaultValue) {
        return bool == null ? defaultValue : bool;
    }


    private static class WatermarkEvent extends PdfPageEventHelper {

        private final Phrase value;
        private final int align;
        private final float x;
        private final float y;
        private final float rotation;
        private final int loop;
        private final int spacing;
        private WatermarkEvent(PrintInfo.WatermarkInfo watermarkInfo) {
            String value = watermarkInfo.getValue();

            float fontSize = toFloat(watermarkInfo.getFontSize(), 60);
            BaseColor color = toColor(watermarkInfo.getRgba());
            if (color == null) {
                color = new BaseColor(180, 180, 180, 40);
            }
            boolean bold = toBoolean(watermarkInfo.getFontBold(), true);
            Font font = new Font(toBaseFont(value, bold), fontSize, Font.NORMAL, color);

            this.value = generateValue(value, bold, font);
            this.align = Element.ALIGN_CENTER;
            this.x = toFloat(watermarkInfo.getX(), 255F);
            this.y = toFloat(watermarkInfo.getY(), 215F);
            this.rotation = toFloat(watermarkInfo.getRotation(), 45F);
            this.loop = toInt(watermarkInfo.getCount(), 3);
            this.spacing = toInt(watermarkInfo.getSpacing(), 200);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            for (int i = 0; i < loop; i++) {
                ColumnText.showTextAligned(canvas, align, value, x, (y + i * spacing), rotation);
            }
        }
    }
}
