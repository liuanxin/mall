package com.github.common.pdf;

import com.github.common.json.JsonUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings({"DuplicatedCode", "unchecked"})
@Slf4j
public class PdfUtil {

    private static final Pattern CN_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
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
            if (log.isErrorEnabled()) {
                log.error(String.format("装载字体(%s)异常", cnFontName), e);
            }
            baseCnFont = BASE_FONT;
        }
        CHINESE_BASE_FONT = baseCnFont;

        BaseFont baseFontCnBold;
        try {
            baseFontCnBold = BaseFont.createFont(cnBoldFontName, cnEncoding, BaseFont.EMBEDDED);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("装载字体(%s)异常", cnBoldFontName), e);
            }
            baseFontCnBold = BASE_FONT_BOLD;
        }
        CHINESE_BASE_FONT_BOLD = baseFontCnBold;
    }

    public static void generatePdfFile(String file, String template, Map<String, Object> data) {
        generatePdfFile(file, JsonUtil.toObject(template, PrintInfo.class), data);
    }
    public static void generatePdfFile(String file, PrintInfo template, Map<String, Object> data) {
        if (template != null) {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                writePdf(template, data, outputStream);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("生成 pdf 文件异常", e);
                }
            }
        }
    }

    public static byte[] generatePdfByte(String template, Map<String, Object> data) {
        return generatePdfByte(JsonUtil.toObject(template, PrintInfo.class), data);
    }
    public static byte[] generatePdfByte(PrintInfo template, Map<String, Object> data) {
        if (template != null) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                writePdf(template, data, outputStream);
                return outputStream.toByteArray();
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("生成 pdf 字节异常", e);
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
            String watermarkValue = template.getWatermarkValue();
            float watermarkFontSize = toFloat(template.getWatermarkFontSize(), 60);
            Font font = new Font(toBaseFont(watermarkValue, true), watermarkFontSize, Font.NORMAL, new BaseColor(245, 245, 245));
            int alignment = Element.ALIGN_CENTER;
            float watermarkX = toFloat(template.getWatermarkX(), 258F);
            float watermarkY = toFloat(template.getWatermarkY(), 221F);
            float rotation = toFloat(template.getWatermarkRotation(), 45F);
            int loop = toInt(template.getWatermarkCount(), 3);
            int spacing = toInt(template.getWatermarkSpacing(), 200);
            writer.setPageEvent(new Watermark(watermarkValue, font, alignment, watermarkX, watermarkY, rotation, loop, spacing));
        }
        document.open();
        PdfContentByte canvas = writer.getDirectContent();

        float offsetX = toFloat(template.getOffsetX(), 0);
        float offsetY = toFloat(template.getOffsetY(), 0);

        List<?> list = template.pageList(data);
        if (list != null) {
            PrintInfo.TableDynamicHead dynamicHead = template.getDynamicHead();
            List<PrintInfo.TableContent> dynamicContent = template.getDynamicContent();
            if (dynamicHead != null && dynamicContent != null && dynamicContent.size() > 0) {
                int total = list.size();
                int pageCount = toInt(dynamicHead.getSinglePageCount(), 10);
                int loopCount = (total % pageCount == 0) ? total / pageCount : (total / pageCount) + 1;
                for (int i = 0; i < loopCount; i++) {
                    int fromIndex = pageCount * i;
                    boolean notLastPage = (i + 1 != loopCount);
                    int toIndex = notLastPage ? (fromIndex + pageCount) : total;
                    draw(i, loopCount, data, template, offsetX, offsetY, canvas);
                    drawDynamicTable(i, pageCount, total, list, fromIndex, toIndex, dynamicHead, dynamicContent, offsetX, offsetY, canvas);
                    if (notLastPage) {
                        document.newPage();
                    }
                }
            }
        } else {
            draw(0, 1, data, template, offsetX, offsetY, canvas);
        }

        document.add(new Chunk(""));
        document.close();
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
                writeTableInfo(canvas, offsetX, offsetY, table, tableContentList, data);
            }
        }
    }

    private static void drawDynamicTable(int page, int pageCount, int size, List<?> list, int fromIndex, int toIndex,
                                         PrintInfo.TableDynamicHead dynamicHead,
                                         List<PrintInfo.TableContent> tableContentList,
                                         float offsetX, float offsetY, PdfContentByte canvas) {
        List<?> pageDataList = list.subList(fromIndex, toIndex);
        if (!pageDataList.isEmpty()) {
            List<Float> fieldWidth = dynamicHead.getFieldWidthList();
            int tableColumnSize = fieldWidth.size();

            float[] totalWidth = new float[tableColumnSize];
            for (int i = 0; i < tableColumnSize; i++) {
                totalWidth[i] = fieldWidth.get(i);
            }
            PdfPTable table = new PdfPTable(tableColumnSize);
            try {
                table.setTotalWidth(totalWidth);
            } catch (DocumentException e) {
                if (log.isErrorEnabled()) {
                    log.error("设置动态表头宽时异常", e);
                }
            }

            writeTableHead(table, dynamicHead.getPrintHead(), dynamicHead.getFontBold(),
                    dynamicHead.getFontSize(), dynamicHead.getBackRgba(), dynamicHead.getHeadList(),
                    dynamicHead.getHeight(), dynamicHead.getBorder(), dynamicHead.getTextAlign());

            for (int i = 0; i < pageDataList.size(); i++) {
                Object obj = pageDataList.get(i);
                if ((obj instanceof Map)) {
                    Map<String, Object> map = (Map<String, Object>) obj;
                    for (PrintInfo.TableContent tableContent : tableContentList) {
                        String suffix = toStr(tableContent.getValueSuffix());
                        String value = toStr(map.get(tableContent.getFieldName())) + suffix;

                        int maxCount = toInt(tableContent.getMaxCount(), 0);
                        if (maxCount > 0 && value.length() > maxCount) {
                            value = value.substring(0, maxCount) + " ...";
                        }

                        PdfPCell cell = new PdfPCell();
                        cell.setMinimumHeight(toInt(tableContent.getHeight(), 20));
                        if (!toBoolean(dynamicHead.getBorder(), false)) {
                            cell.setBorder(PdfPCell.NO_BORDER);
                        }
                        cell.setUseAscender(true);
                        cell.setHorizontalAlignment(toInt(tableContent.getTextAlign(), Element.ALIGN_LEFT));
                        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

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
                                case TABLE_LINE_INDEX:
                                    String lineIndex = (pageCount * page + 1 + i) + suffix;
                                    cell.setPhrase(new Phrase(toStr(lineIndex), toFont(value, fontBold, fontSize)));
                                    break;
                                case TABLE_LINE_COUNT:
                                    String lineCount = size + suffix;
                                    cell.setPhrase(new Phrase(toStr(lineCount), toFont(lineCount, fontBold, fontSize)));
                                    break;
                                case TABLE_LINE_INDEX_COUNT:
                                    String lineIndexCount = (pageCount * page + 1 + i) + "/" + size + suffix;
                                    cell.setPhrase(new Phrase(toStr(lineIndexCount), toFont(lineIndexCount, fontBold, fontSize)));
                                    break;
                                default:
                                    cell.setPhrase(new Phrase(toStr(value), toFont(value, fontBold, fontSize)));
                                    break;
                            }
                        } else {
                            cell.setPhrase(new Phrase(toStr(value), toFont(value, fontBold, fontSize)));
                        }
                        table.addCell(cell);
                    }
                }
            }
            table.writeSelectedRows(0, -1, (dynamicHead.getX() + offsetX), (dynamicHead.getY() + offsetY), canvas);
        }
    }

    private static void writeDataContent(int page, int len, PdfContentByte canvas, float offsetX, float offsetY,
                                         PrintInfo.DataContent dataContent, Map<String, Object> data) {
        String suffix = toStr(dataContent.getValueSuffix());
        String value = toStr(dataContent.getValue()) + toStr(data.get(toStr(dataContent.getFieldName()))) + suffix;
        float x = toFloat(dataContent.getX(), 0) + offsetX;
        float y = toFloat(dataContent.getY(), 0) + offsetY;

        float fontSize = toFloat(dataContent.getFontSize(), 10);
        int splitCount = toInt(dataContent.getSplitCount(), 0);
        int maxLine = toInt(dataContent.getMaxLine(), 0);
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
                            if (log.isErrorEnabled()) {
                                log.error("写 barcode 异常", e);
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
                            if (log.isErrorEnabled()) {
                                log.error("写 qrcode 异常", e);
                            }
                        }
                    }
                    return;
                case TABLE_LINE_INDEX:
                    value = (page + 1) + suffix;
                    break;
                case TABLE_LINE_COUNT:
                    value = len + suffix;
                    break;
                case TABLE_LINE_INDEX_COUNT:
                    value = (page + 1) + "/" + len + suffix;
                    break;
            }
        }

        if (!"".equals(value.trim())) {
            value = value.trim();
            int valueLen = value.length();
            int loopCount;
            if (splitCount > 0 && valueLen > splitCount) {
                int calcCount = (valueLen % splitCount == 0) ? valueLen / splitCount : (valueLen / splitCount) + 1;
                if (maxLine > 0 && calcCount > maxLine) {
                    loopCount = maxLine;
                } else {
                    loopCount = calcCount;
                }
            } else {
                loopCount = 1;
            }

            canvas.beginText();
            canvas.setFontAndSize(toBaseFont(value, bold), fontSize);
            for (int i = 0; i < loopCount; i++) {
                canvas.showTextAligned(textAlign, toStr(value), x, y, 0);
                y -= (fontSize + 4);
            }
            canvas.endText();
        }
    }

    private static void writeTableInfo(PdfContentByte canvas, float offsetX, float offsetY,
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

        List<Float> fieldWidth = tableHead.getFieldWidthList();
        int tableColumnSize = fieldWidth.size();

        float[] totalWidth = new float[tableColumnSize];
        for (int i = 0; i < tableColumnSize; i++) {
            totalWidth[i] = fieldWidth.get(i);
        }
        PdfPTable table = new PdfPTable(tableColumnSize);
        try {
            table.setTotalWidth(totalWidth);
        } catch (DocumentException e) {
            if (log.isErrorEnabled()) {
                log.error("设置表头宽时异常", e);
            }
        }

        writeTableHead(table, tableHead.getPrintHead(), tableHead.getFontBold(), tableHead.getFontSize(),
                tableHead.getBackRgba(), tableHead.getHeadList(), tableHead.getHeight(),
                tableHead.getBorder(), tableHead.getTextAlign());

        int size = ((List<?>) list).size();
        for (int i = 0; i < size; i++) {
            Object obj = ((List<?>) list).get(i);
            if ((obj instanceof Map)) {
                Map<String, Object> map = (Map<String, Object>) obj;
                for (PrintInfo.TableContent tableContent : tableContentList) {
                    String suffix = toStr(tableContent.getValueSuffix());
                    String value = toStr(map.get(tableContent.getFieldName())) + suffix;

                    int maxCount = toInt(tableContent.getMaxCount(), 0);
                    if (maxCount > 0 && value.length() > maxCount) {
                        value = value.substring(0, maxCount) + " ...";
                    }

                    PdfPCell cell = new PdfPCell();
                    cell.setMinimumHeight(toInt(tableContent.getHeight(), 20));
                    if (!toBoolean(tableHead.getBorder(), false)) {
                        cell.setBorder(PdfPCell.NO_BORDER);
                    }
                    cell.setUseAscender(true);
                    cell.setHorizontalAlignment(toInt(tableContent.getTextAlign(), Element.ALIGN_LEFT));
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
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
                            case TABLE_LINE_INDEX:
                                String lineIndex = (i + 1) + suffix;
                                cell.setPhrase(new Phrase(toStr(lineIndex), toFont(lineIndex, fontBold, fontSize)));
                                break;
                            case TABLE_LINE_COUNT:
                                String lineCount = size + suffix;
                                cell.setPhrase(new Phrase(toStr(lineCount), toFont(lineCount, fontBold, fontSize)));
                                break;
                            case TABLE_LINE_INDEX_COUNT:
                                String lineIndexCount = (i + 1) + "/" + size + suffix;
                                cell.setPhrase(new Phrase(toStr(lineIndexCount), toFont(lineIndexCount, fontBold, fontSize)));
                                break;
                            default:
                                cell.setPhrase(new Phrase(toStr(value), toFont(value, fontBold, fontSize)));
                                break;
                        }
                    } else {
                        cell.setPhrase(new Phrase(toStr(value), toFont(value, fontBold, fontSize)));
                    }
                    table.addCell(cell);
                }
            }
        }
        table.writeSelectedRows(0, -1, (tableHead.getX() + offsetX), (tableHead.getY() + offsetY), canvas);
    }

    private static void writeTableHead(PdfPTable table, Boolean printHead, Boolean bold,
                                       Float fontSize2, List<Integer> backRgba, List<String> headList,
                                       Integer height, Boolean border, Integer textAlign) {
        if (toBoolean(printHead, true)) {
            BaseFont font = toBoolean(bold, false) ? CHINESE_BASE_FONT_BOLD : CHINESE_BASE_FONT;
            Font headFont = new Font(font, toFloat(fontSize2, 10));
            BaseColor background = toColor(backRgba);
            for (String head : headList) {
                PdfPCell cell = new PdfPCell(new Phrase(toStr(head), headFont));
                cell.setMinimumHeight(toInt(height, 15));
                if (!toBoolean(border, false)) {
                    cell.setBorder(PdfPCell.NO_BORDER);
                }
                if (background != null) {
                    cell.setBackgroundColor(background);
                }
                cell.setUseAscender(true);
                cell.setHorizontalAlignment(toInt(textAlign, Element.ALIGN_LEFT));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }
        }
    }

    private static Image generateBarCode(PdfContentByte canvas, float barCodeTextSize, float barCodeBaseLine,
                                         float barCodeWidth, float barCodeHeight, String value) {
        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(value);
            barcode.setSize(barCodeTextSize);
            barcode.setBaseline(barCodeBaseLine);
            Image image = barcode.createImageWithBarcode(canvas, null, null);
            image.scaleAbsolute(barCodeWidth, barCodeHeight);
            return image;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("生成 barCode 异常", e);
            }
            return null;
        }
    }

    private static Image generateQrCode(float qrCodeWidth, float qrCodeHeight, String value) {
        try {
            Image qrcodeImage = new BarcodeQRCode(value, (int) qrCodeWidth, (int) qrCodeHeight, HINTS).getImage();
            qrcodeImage.scaleAbsolute(qrCodeWidth, qrCodeHeight);
            return qrcodeImage;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("生成 qrCode 异常", e);
            }
            return null;
        }
    }

    private static BaseFont toBaseFont(String value, boolean bold) {
        if (value != null && CN_PATTERN.matcher(value).find()) {
            return bold ? CHINESE_BASE_FONT_BOLD : CHINESE_BASE_FONT;
        } else {
            return bold ? BASE_FONT_BOLD : BASE_FONT;
        }
    }
    private static Font toFont(String value, boolean bold, float fontSize) {
        BaseFont baseFont = toBaseFont(value, bold);
        return bold ? new Font(baseFont, fontSize, Font.BOLD) : new Font(baseFont, fontSize);
    }
    private static BaseColor toColor(List<Integer> rgba) {
        if (rgba != null && !rgba.isEmpty() && rgba.size() >= 3) {
            int red = toColorInt(rgba.get(0));
            int green = toColorInt(rgba.get(1));
            int blue = toColorInt(rgba.get(2));

            if (red > 0 && green > 0 && blue > 0) {
                int alpha = (rgba.size() > 3) ? toColorInt(rgba.get(3)) : 0;
                return alpha > 0 ? new BaseColor(red, green, blue, alpha) : new BaseColor(red, green, blue);
            }
        }
        return null;
    }
    private static int toColorInt(Integer num) {
        return Math.min(toInt(num, 0), 255);
    }

    private static float toBaseLine(Float num) {
        return num == null ? (float) 10 : num;
    }
    private static String toStr(Object obj) {
        return obj == null ? "" : obj.toString();
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


    private static class Watermark extends PdfPageEventHelper {

        private final Phrase value;
        private final int alignment;
        private final float x;
        private final float y;
        private final float rotation;
        private final int loop;
        private final int spacing;
        private Watermark(String value, Font font, Integer alignment, Float x, Float y, Float rotation, int loop, int spacing) {
            this.value = new Phrase(value, font);
            this.alignment = alignment;
            this.x = toFloat(x, 0);
            this.y = toFloat(y, 0);
            this.rotation = toFloat(rotation, 0);
            this.loop = loop;
            this.spacing = spacing;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            for (int i = 0; i < loop; i++) {
                ColumnText.showTextAligned(canvas, alignment, value, x, (y + i * spacing), rotation);
            }
        }
    }
}