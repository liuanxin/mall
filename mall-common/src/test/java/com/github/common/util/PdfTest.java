package com.github.common.util;

import com.github.common.date.DateUtil;
import com.github.common.pdf.PdfUtil;
import com.github.common.pdf.PrintInfo;
import com.itextpdf.text.Element;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

public class PdfTest {

    @Test
    public void sku() {
        PrintInfo print = new PrintInfo();
        // print.setWidth(595F);
        // print.setHeight(842F);

        // PrintInfo.WatermarkInfo watermark = new PrintInfo.WatermarkInfo();
        // watermark.setValue("这里有水印");
        // print.setWatermark(watermark);

        List<PrintInfo.DataContent> contentList = new ArrayList<>();
        PrintInfo.DataContent phc0 = new PrintInfo.DataContent();
        phc0.setX(270F);
        phc0.setY(800F);
        phc0.setValue("入库清单");
        phc0.setFontSize(26F);
        phc0.setFontBold(true);
        contentList.add(phc0);

        PrintInfo.DataContent phc1 = new PrintInfo.DataContent();
        phc1.setX(440F);
        phc1.setY(800F);
        phc1.setFontSize(12F);
        phc1.setFontBold(true);
        phc1.setFieldType(PrintInfo.PlaceholderType.INDEX_COUNT);
        phc1.setValueSuffix(" p");
        contentList.add(phc1);

        PrintInfo.DataContent phc2 = new PrintInfo.DataContent();
        phc2.setX(440F);
        phc2.setY(780F);
        phc2.setValue("[GC]");
        phc2.setFontBold(true);
        phc2.setFontSize(12F);
        phc2.setRgba(Arrays.asList(255, 0, 0));
        contentList.add(phc2);

        PrintInfo.DataContent phc3 = new PrintInfo.DataContent();
        phc3.setX(35F);
        phc3.setY(780F);
        phc3.setFieldType(PrintInfo.PlaceholderType.BARCODE);
        phc3.setFieldName("barCode");
        phc3.setBarCodeTextSize(0F);
        phc3.setCodeWidth(165F);
        phc3.setCodeHeight(30F);
        contentList.add(phc3);
        print.setContentInfo(contentList);

        List<PrintInfo.TableInfo> tableList = new ArrayList<>();
        PrintInfo.TableInfo pt1 = new PrintInfo.TableInfo();
        PrintInfo.TableHead key1 = new PrintInfo.TableHead();
        key1.setX(35);
        key1.setY(768);
        key1.setBorder(false);
        key1.setPrintHead(false);
        key1.setFieldWidthList(Arrays.asList(55F, 180F, 68F, 200F));
        key1.setFieldName("orderInfo");
        pt1.setKey(key1);

        List<PrintInfo.TableContent> value1 = new ArrayList<>();
        PrintInfo.TableContent ptc11 = new PrintInfo.TableContent();
        ptc11.setFieldName("c1");
        ptc11.setFontSize(12F);
        value1.add(ptc11);

        PrintInfo.TableContent ptc12 = new PrintInfo.TableContent();
        ptc12.setFieldName("c2");
        ptc12.setFontSize(12F);
        value1.add(ptc12);

        PrintInfo.TableContent ptc13 = new PrintInfo.TableContent();
        ptc13.setFieldName("c3");
        ptc13.setFontSize(12F);
        value1.add(ptc13);

        PrintInfo.TableContent ptc14 = new PrintInfo.TableContent();
        ptc14.setFieldName("c4");
        ptc14.setFontSize(12F);
        value1.add(ptc14);

        pt1.setValue(value1);
        tableList.add(pt1);


        PrintInfo.TableInfo pt2 = new PrintInfo.TableInfo();
        PrintInfo.TableHead key2 = new PrintInfo.TableHead();
        key2.setX(35);
        key2.setY(706);
        key2.setBorder(false);
        key2.setPrintHead(false);
        key2.setFieldWidthList(Arrays.asList(55F, 470F));
        key2.setFieldName("remarkInfo");
        pt2.setKey(key2);

        List<PrintInfo.TableContent> value2 = new ArrayList<>();
        PrintInfo.TableContent ptc21 = new PrintInfo.TableContent();
        ptc21.setFieldName("name");
        ptc21.setFontSize(12F);
        value2.add(ptc21);

        PrintInfo.TableContent ptc22 = new PrintInfo.TableContent();
        ptc22.setFieldName("value");
        ptc22.setFontSize(12F);
        ptc22.setSpace(true);
        value2.add(ptc22);

        pt2.setValue(value2);
        tableList.add(pt2);
        print.setTableInfo(tableList);

        PrintInfo.TableDynamicHead dynamicTableKey = new PrintInfo.TableDynamicHead();
        dynamicTableKey.setX(35);
        dynamicTableKey.setY(645);
        dynamicTableKey.setFieldName("productList");
        dynamicTableKey.setFieldWidthList(Arrays.asList(30F, 70F, 68F, 70F, 54F, 54F, 54F, 50F, 40F, 40F));
        dynamicTableKey.setHeadList(Arrays.asList("箱号", "产品代码", "中文申报品名", "产品重量(KG)", "长度(CM)", "宽度(CM)", "高度(CM)", "货物属性", "预期数", "收货数"));
        dynamicTableKey.setBackRgba(Arrays.asList(220, 220, 220));
        dynamicTableKey.setTextAlign(Element.ALIGN_CENTER);
        dynamicTableKey.setFontSize(10F);
        dynamicTableKey.setFontBold(true);
        dynamicTableKey.setBorder(true);
        dynamicTableKey.setHeadHeight(24);
        dynamicTableKey.setContentHeight(28);
        dynamicTableKey.setSinglePageCount(20);
        print.setDynamicHead(dynamicTableKey);

        List<PrintInfo.TableContent> dynamicTableValue = new ArrayList<>();

        PrintInfo.TableContent dptc1 = new PrintInfo.TableContent();
        dptc1.setFieldName("boxNum");
        dptc1.setTextAlign(Element.ALIGN_CENTER);
        dptc1.setFontSize(8F);
        dynamicTableValue.add(dptc1);

        PrintInfo.TableContent dptc2 = new PrintInfo.TableContent();
        dptc2.setFieldName("productCode");
        dptc2.setTextAlign(Element.ALIGN_CENTER);
        dptc2.setFontSize(8F);
        dynamicTableValue.add(dptc2);

        PrintInfo.TableContent dptc3 = new PrintInfo.TableContent();
        dptc3.setFieldName("cnName");
        dptc3.setTextAlign(Element.ALIGN_CENTER);
        dptc3.setFontSize(8F);
        dynamicTableValue.add(dptc3);

        PrintInfo.TableContent dptc4 = new PrintInfo.TableContent();
        dptc4.setFieldName("weight");
        dptc4.setTextAlign(Element.ALIGN_CENTER);
        dptc4.setFontSize(8F);
        dynamicTableValue.add(dptc4);

        PrintInfo.TableContent dptc5 = new PrintInfo.TableContent();
        dptc5.setFieldName("length");
        dptc5.setTextAlign(Element.ALIGN_CENTER);
        dptc5.setFontSize(8F);
        dynamicTableValue.add(dptc5);

        PrintInfo.TableContent dptc6 = new PrintInfo.TableContent();
        dptc6.setFieldName("width");
        dptc6.setTextAlign(Element.ALIGN_CENTER);
        dptc6.setFontSize(8F);
        dynamicTableValue.add(dptc6);

        PrintInfo.TableContent dptc7 = new PrintInfo.TableContent();
        dptc7.setFieldName("height");
        dptc7.setTextAlign(Element.ALIGN_CENTER);
        dptc7.setFontSize(8F);
        dynamicTableValue.add(dptc7);

        PrintInfo.TableContent dptc8 = new PrintInfo.TableContent();
        dptc8.setFieldName("property");
        dptc8.setTextAlign(Element.ALIGN_CENTER);
        dptc8.setFontSize(8F);
        dynamicTableValue.add(dptc8);

        PrintInfo.TableContent dptc9 = new PrintInfo.TableContent();
        dptc9.setFieldName("forecastNum");
        dptc9.setTextAlign(Element.ALIGN_CENTER);
        dptc9.setFontSize(8F);
        dynamicTableValue.add(dptc9);

        PrintInfo.TableContent dptc10 = new PrintInfo.TableContent();
        dptc10.setFieldName("receiveNum");
        dptc10.setTextAlign(Element.ALIGN_CENTER);
        dptc10.setFontSize(8F);
        dynamicTableValue.add(dptc10);
        print.setDynamicContent(dynamicTableValue);


        Map<String, Object> data = new HashMap<>();
        data.put("barCode", "RV000013-150721-0004");
        data.put("remarkInfo", Collections.singletonList(A.maps(
                "name", "备注: ",
                "value", "显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字显示120个包含中文的字"
        )));
        data.put("orderInfo", Arrays.asList(
                A.maps("c1", "入库单号: ", "c2", "RV000013-150721-0004", "c3", "创建时间: ", "c4", DateUtil.formatDateTime(LocalDateTime.now())),
                A.maps("c1", "目的仓库: ", "c2", "USEA", "c3", "客户代码: ", "c4", "000013"),
                A.maps("c1", "跟踪号: ", "c2", "YT1520216989500749", "c3", "客户参考号: ", "c4", "JD2015002")
        ));
        List<Map<Object, Object>> list = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            list.add(A.maps(
                    "boxNum", "1",
                    "productCode", "000013-1399379",
                    "cnName", "（仓库自配转换插）手持式电动旋盖机 欧规 220V (采购下单备注英文说明书)（12个一箱）（散单采购200元包邮）",
                    "weight", "0.030",
                    "length", "15.00",
                    "width", "12.00",
                    "height", "2.00",
                    "property", "普货",
                    "forecastNum", "30",
                    "receiveNum", "30"
            ));
            list.add(A.maps(
                    "boxNum", "1",
                    "productCode", "000013-1392179",
                    "cnName", "夏日花草（合金支架120cm ）浴桶+坐垫+保温盖+10个浴袋（下单备注4个一箱发货）（拍单发图）",
                    "weight", "0.135",
                    "length", "15.00",
                    "width", "12.00",
                    "height", "2.00",
                    "property", "普货",
                    "forecastNum", "17",
                    "receiveNum", "15"
            ));
        }
        data.put("productList", list);

        // System.out.println("===================");
        // System.out.println(JsonUtil.toJson(print));
        // System.out.println("===================");
        // System.out.println(JsonUtil.toJson(data));
        // System.out.println("===================");

        String file = "/home/ty/test-sku.pdf";
        PdfUtil.generatePdfFile(file, print, data);
    }

    @Test
    public void box() {
        PrintInfo print = new PrintInfo();
        // print.setWidth(595F);
        // print.setHeight(842F);

        // PrintInfo.WatermarkInfo watermark = new PrintInfo.WatermarkInfo();
        // watermark.setValue("这里有水印");
        // print.setWatermark(watermark);

        List<PrintInfo.DataContent> holdContentList = new ArrayList<>();
        PrintInfo.DataContent phc0 = new PrintInfo.DataContent();
        phc0.setX(190F);
        phc0.setY(800F);
        phc0.setValue("Packing List");
        phc0.setFontSize(22F);
        phc0.setFontBold(true);
        holdContentList.add(phc0);

        PrintInfo.DataContent phc1 = new PrintInfo.DataContent();
        phc1.setX(440F);
        phc1.setY(800F);
        phc1.setFontSize(12F);
        phc1.setFontBold(true);
        phc1.setFieldType(PrintInfo.PlaceholderType.INDEX_COUNT);
        phc1.setValueSuffix(" p");
        holdContentList.add(phc1);

        PrintInfo.DataContent phc2 = new PrintInfo.DataContent();
        phc2.setX(75F);
        phc2.setY(755F);
        phc2.setFieldName("warehouse");
        phc2.setFontBold(true);
        phc2.setFontSize(14F);
        holdContentList.add(phc2);

        PrintInfo.DataContent phc3 = new PrintInfo.DataContent();
        phc3.setX(35F);
        phc3.setY(690F);
        phc3.setFieldType(PrintInfo.PlaceholderType.BARCODE);
        phc3.setFieldName("barCode");
        phc3.setCodeWidth(165F);
        phc3.setCodeHeight(60F);
        holdContentList.add(phc3);

        // PrintInfo.DataContent phc4 = new PrintInfo.DataContent();
        // phc4.setFieldType(PrintInfo.PlaceholderType.LINE);
        // phc4.setLineTrack(Arrays.asList(
        //         Arrays.asList(10F, 832F),
        //         Arrays.asList(585F, 832F),
        //         Arrays.asList(585F, 10F),
        //         Arrays.asList(10F, 10F)
        // ));
        // phc4.setLineWidth(2F);
        // phc4.setLineGray(0.75F);
        // holdContentList.add(phc4);
        print.setContentInfo(holdContentList);

        List<PrintInfo.TableInfo> tableList = new ArrayList<>();
        PrintInfo.TableInfo pt1 = new PrintInfo.TableInfo();
        PrintInfo.TableHead key1 = new PrintInfo.TableHead();
        key1.setX(306);
        key1.setY(768);
        key1.setBorder(false);
        key1.setPrintHead(false);
        key1.setFieldWidthList(Arrays.asList(60F, 180F));
        key1.setFieldName("orderInfo");
        pt1.setKey(key1);

        List<PrintInfo.TableContent> value1 = new ArrayList<>();
        PrintInfo.TableContent ptc11 = new PrintInfo.TableContent();
        ptc11.setFieldName("name");
        ptc11.setTextAlign(Element.ALIGN_RIGHT);
        value1.add(ptc11);

        PrintInfo.TableContent ptc12 = new PrintInfo.TableContent();
        ptc12.setFieldName("value");
        // ptc12.setMaxCount(20);
        value1.add(ptc12);

        pt1.setValue(value1);
        tableList.add(pt1);

        PrintInfo.TableInfo pt2 = new PrintInfo.TableInfo();
        PrintInfo.TableHead key2 = new PrintInfo.TableHead();
        key2.setX(35);
        key2.setY(60);
        key2.setPrintHead(false);
        key2.setFieldWidthList(Arrays.asList(155F, 90F, 120F, 155F));
        key2.setFieldName("printInfo");
        pt2.setKey(key2);

        List<PrintInfo.TableContent> value2 = new ArrayList<>();
        PrintInfo.TableContent ptc21 = new PrintInfo.TableContent();
        ptc21.setFieldName("printTime");
        value2.add(ptc21);

        PrintInfo.TableContent ptc22 = new PrintInfo.TableContent();
        ptc22.setFieldName("boxTotal");
        value2.add(ptc22);

        PrintInfo.TableContent ptc23 = new PrintInfo.TableContent();
        ptc23.setFieldName("receiver");
        value2.add(ptc23);

        PrintInfo.TableContent ptc24 = new PrintInfo.TableContent();
        ptc24.setFieldName("receiveTime");
        value2.add(ptc24);

        pt2.setValue(value2);
        tableList.add(pt2);
        print.setTableInfo(tableList);

        PrintInfo.TableDynamicHead dynamicTableKey = new PrintInfo.TableDynamicHead();
        dynamicTableKey.setX(35);
        dynamicTableKey.setY(645);
        dynamicTableKey.setFieldName("productList");
        // dynamicTableKey.setFieldWidthList(Arrays.asList(25F, 60F, 58F, 55F, 55F, 53F, 45F, 105F, 68F));
        dynamicTableKey.setFieldWidthList(Arrays.asList(60F, 93F, 55F, 55F, 53F, 45F, 95F, 68F));
        dynamicTableKey.setHeadList(Arrays.asList(/*"序号", */"分拣码*箱数", "SKU", "预报总数量", "实收总数量", "SKU总箱数", "实收箱数", "单品规格(CM)", "单品重量(KG)"));
        dynamicTableKey.setBackRgba(Arrays.asList(220, 220, 220));
        dynamicTableKey.setTextAlign(Element.ALIGN_CENTER);
        dynamicTableKey.setFontSize(10F);
        dynamicTableKey.setFontBold(true);
        dynamicTableKey.setBorder(true);
        dynamicTableKey.setHeadHeight(18);
        dynamicTableKey.setContentHeight(26);
        dynamicTableKey.setSinglePageCount(20);
        print.setDynamicHead(dynamicTableKey);

        List<PrintInfo.TableContent> dynamicTableValue = new ArrayList<>();
        // PrintInfo.TableContent dptc0 = new PrintInfo.TableContent();
        // dptc0.setFieldType(PrintInfo.PlaceholderType.INDEX);
        // dptc0.setTextAlign(Element.ALIGN_CENTER);
        // dynamicTableValue.add(dptc0);

        PrintInfo.TableContent dptc1 = new PrintInfo.TableContent();
        dptc1.setFieldName("stockCodeAndBoxNum");
        dptc1.setTextAlign(Element.ALIGN_CENTER);
        dptc1.setFontSize(8F);
        dynamicTableValue.add(dptc1);

        PrintInfo.TableContent dptc2 = new PrintInfo.TableContent();
        dptc2.setFieldName("productCode");
        dptc2.setTextAlign(Element.ALIGN_CENTER);
        dptc2.setFontSize(8F);
        dynamicTableValue.add(dptc2);

        PrintInfo.TableContent dptc3 = new PrintInfo.TableContent();
        dptc3.setFieldName("forecastNum");
        dptc3.setTextAlign(Element.ALIGN_CENTER);
        dptc3.setFontSize(8F);
        dptc3.setRgba(Arrays.asList(255, 0, 0));
        dynamicTableValue.add(dptc3);

        PrintInfo.TableContent dptc4 = new PrintInfo.TableContent();
        dptc4.setFieldName("actualNum");
        dptc4.setTextAlign(Element.ALIGN_CENTER);
        dptc4.setFontSize(8F);
        dynamicTableValue.add(dptc4);

        PrintInfo.TableContent dptc5 = new PrintInfo.TableContent();
        dptc5.setFieldName("skuNum");
        dptc5.setTextAlign(Element.ALIGN_CENTER);
        dptc5.setFontSize(8F);
        dynamicTableValue.add(dptc5);

        PrintInfo.TableContent dptc6 = new PrintInfo.TableContent();
        dptc6.setFieldName("actualSkuNum");
        dptc6.setTextAlign(Element.ALIGN_CENTER);
        dptc6.setFontSize(8F);
        dynamicTableValue.add(dptc6);

        PrintInfo.TableContent dptc7 = new PrintInfo.TableContent();
        dptc7.setFieldName("size");
        dptc7.setTextAlign(Element.ALIGN_CENTER);
        dptc7.setFontSize(8F);
        dynamicTableValue.add(dptc7);

        PrintInfo.TableContent dptc8 = new PrintInfo.TableContent();
        dptc8.setFieldName("weight");
        dptc8.setTextAlign(Element.ALIGN_CENTER);
        dptc8.setFontSize(8F);
        dynamicTableValue.add(dptc8);
        print.setDynamicContent(dynamicTableValue);

        Map<String, Object> data = new HashMap<>();
        data.put("warehouse", "[GC] USEA");
        data.put("barCode", "RV000014-150807-0003");
        data.put("orderInfo", Arrays.asList(
                A.maps("name", "客户代码:", "value", "G666"),
                A.maps("name", "跟踪号:", "value", "X7832178"),
                A.maps("name", "参考编号:", "value", "G666-211204-5678"),
                A.maps("name", "创建时间:", "value", "2020-11-29 12:23:56"),
                A.maps("name", "备注:", "value", "很长「我人有的和主产不为这工要在地一上是中国经以发了民同」的描述")
        ));
        data.put("printInfo", Collections.singletonList(
                A.maps(
                        "printTime", "打印时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                        "boxTotal", "总箱数: 1",
                        "receiver", "收货员: _______________",
                        "receiveTime", "收货时间:"
                )
        ));
        List<Map<Object, Object>> list = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            list.add(A.maps(
                    "stockCodeAndBoxNum", "0*1",
                    "productCode", "00014-A4B01",
                    "forecastNum", "40",
                    "actualNum", "40",
                    "skuNum", "1",
                    "actualSkuNum", "1",
                    "size", "16.00*16.00*4.50",
                    "weight", "0.200"
            ));
            list.add(A.maps(
                    "stockCodeAndBoxNum", "0*1",
                    "abcde", "fdsa",
                    "productCode", "00014-A4W01",
                    "forecastNum", "50",
                    "actualNum", "50",
                    "skuNum", "1",
                    "actualSkuNum", "1",
                    "size", "16.00*16.00*4.50",
                    "weight", "0.270"
            ));
        }
        data.put("productList", list);

        // System.out.println("===================");
        // System.out.println(JsonUtil.toJson(print)); // 模板
        // System.out.println("===================");
        // System.out.println(JsonUtil.toJson(data)); // 数据
        // System.out.println("===================");

        String file = "/home/ty/test-box.pdf";
        PdfUtil.generatePdfFile(file, print, data);

        // String encode = Encrypt.base64Encode(PdfUtil.generatePdfByte(print, data));
        // System.out.println(encode);
        // System.out.println(encode.length());

        // String compress = U.compress(encode);
        // System.out.println(compress);
        // System.out.println(compress.length());
    }

    @Test
    public void product() {
        PrintInfo print = new PrintInfo();
        print.setWidth(198F);
        print.setHeight(84F);

        List<PrintInfo.DataContent> tableList = new ArrayList<>();
        PrintInfo.DataContent dc1 = new PrintInfo.DataContent();
        dc1.setX(151F);
        dc1.setY(5F);
        dc1.setFieldName("qrCode");
        dc1.setFieldType(PrintInfo.PlaceholderType.QRCODE);
        dc1.setTextAlign(Element.ALIGN_RIGHT);
        dc1.setCodeWidth(50F);
        dc1.setCodeHeight(50F);
        tableList.add(dc1);
        print.setDynamicContentKey("dynamicInfo");
        print.setDynamicContentList(tableList);


        PrintInfo.TableDynamicHead dynamicTableKey = new PrintInfo.TableDynamicHead();
        dynamicTableKey.setX(10F);
        dynamicTableKey.setY(80F);
        dynamicTableKey.setFieldName("dynamicTable");
        dynamicTableKey.setFieldWidthList(List.of(178F));
        dynamicTableKey.setPrintHead(false);
        dynamicTableKey.setSinglePageCount(1);
        print.setDynamicHead(dynamicTableKey);

        List<PrintInfo.TableContent> dynamicTableValue = new ArrayList<>();
        PrintInfo.TableContent dptc1 = new PrintInfo.TableContent();
        dptc1.setFieldName("barCode");
        dptc1.setFieldType(PrintInfo.PlaceholderType.BARCODE);
        dptc1.setTextAlign(Element.ALIGN_CENTER);
        dptc1.setBarCodeBaseLine(-5F);
        dptc1.setCodeWidth(178F);
        dptc1.setCodeHeight(70F);
        dynamicTableValue.add(dptc1);
        print.setDynamicContent(dynamicTableValue);


        Map<String, Object> data = new HashMap<>();

        List<Map<Object, Object>> list1 = new ArrayList<>();
        List<Map<Object, Object>> list2 = new ArrayList<>();

        list1.add(A.maps("qrCode", "G1082-AUTOTEST9336290"));
        list1.add(A.maps("qrCode", "G1082-AUTOTEST9336291"));

        list2.add(A.maps("barCode", "G1082-AUTOTEST9336290"));
        list2.add(A.maps("barCode", "G1082-AUTOTEST9336291"));

        data.put("dynamicInfo", list1);
        data.put("dynamicTable", list2);

        String file = "/home/ty/test-product.pdf";
        PdfUtil.generatePdfFile(file, print, data);
    }

    @Test
    public void num() {
        PrintInfo print = new PrintInfo();
        print.setWidth(284F);
        print.setHeight(170F);

        List<PrintInfo.DataContent> contentList = new ArrayList<>();

        PrintInfo.DataContent dc3 = new PrintInfo.DataContent();
        dc3.setFieldType(PrintInfo.PlaceholderType.QRCODE);
        dc3.setX(8F);
        dc3.setY(105F);
        dc3.setValue("中文二维码");
        dc3.setCodeWidth(58F);
        dc3.setCodeHeight(58F);
        contentList.add(dc3);

        PrintInfo.DataContent dc1 = new PrintInfo.DataContent();
        dc1.setFieldType(PrintInfo.PlaceholderType.LINE);
        dc1.setLineTrack(List.of(List.of(10F, 160F), List.of(274F, 160F), List.of(274F, 10F), List.of(10F, 10F)));
        dc1.setLineWidth(1F);
        dc1.setLineGray(0.6F);
        contentList.add(dc1);

        PrintInfo.DataContent dc2 = new PrintInfo.DataContent();
        dc2.setFieldType(PrintInfo.PlaceholderType.LINE);
        dc2.setLineTrack(List.of(List.of(12F, 108F), List.of(272F, 108F)));
        dc2.setLineWidth(1F);
        dc2.setLineGray(0.1F);
        contentList.add(dc2);

        PrintInfo.DataContent dc4 = new PrintInfo.DataContent();
        dc4.setX(15F);
        dc4.setY(15F);
        dc4.setValuePrefix("[");
        dc4.setFieldType(PrintInfo.PlaceholderType.INDEX);
        dc4.setValueSuffix("]");
        contentList.add(dc4);

        PrintInfo.DataContent dc5 = new PrintInfo.DataContent();
        dc5.setX(238F);
        dc5.setY(15F);
        dc5.setValue("China");
        contentList.add(dc5);

        PrintInfo.DataContent dc6 = new PrintInfo.DataContent();
        dc6.setX(170F);
        dc6.setY(120F);
        dc6.setFieldName("countryCode");
        dc6.setFontSize(22F);
        dc6.setFontBold(true);
        contentList.add(dc6);
        print.setContentInfo(contentList);


        List<PrintInfo.DataContent> tableList = new ArrayList<>();
        PrintInfo.DataContent pdc2 = new PrintInfo.DataContent();
        pdc2.setX(45F);
        pdc2.setY(75F);
        pdc2.setFieldName("barCode");
        pdc2.setFieldType(PrintInfo.PlaceholderType.BARCODE);
        pdc2.setCodeWidth(200F);
        pdc2.setCodeHeight(30F);
        pdc2.setBarCodeBaseLine(10F);
        tableList.add(pdc2);
        print.setDynamicContentKey("dynamicInfo");
        print.setDynamicContentList(tableList);

        PrintInfo.TableDynamicHead dynamicTableKey = new PrintInfo.TableDynamicHead();
        dynamicTableKey.setX(15F);
        dynamicTableKey.setY(70F);
        dynamicTableKey.setFieldName("dt");
        dynamicTableKey.setFieldWidthList(List.of(214F, 40F));
        dynamicTableKey.setHeadList(List.of("Multi(1)", "PCS"));
        dynamicTableKey.setTextAlign(Element.ALIGN_CENTER);
        dynamicTableKey.setBackRgba(List.of(120, 120, 120, 120));
        dynamicTableKey.setSinglePageCount(1);
        dynamicTableKey.setBorder(true);
        print.setDynamicHead(dynamicTableKey);

        List<PrintInfo.TableContent> dynamicTableValue = new ArrayList<>();
        PrintInfo.TableContent dptc1 = new PrintInfo.TableContent();
        dptc1.setFieldName("num");
        dptc1.setTextAlign(Element.ALIGN_CENTER);
        dynamicTableValue.add(dptc1);

        PrintInfo.TableContent dptc2 = new PrintInfo.TableContent();
        dptc2.setFieldName("pcs");
        dptc2.setTextAlign(Element.ALIGN_CENTER);
        dynamicTableValue.add(dptc2);
        print.setDynamicContent(dynamicTableValue);


        Map<String, Object> data = new HashMap<>();
        data.put("countryCode", "UK");

        List<Map<Object, Object>> list1 = new ArrayList<>();
        list1.add(A.maps("barCode", "G1082-AUTOTEST9336290"));
        list1.add(A.maps("barCode", "G1082-AUTOTEST9336291"));
        data.put("dynamicInfo", list1);

        List<Map<Object, Object>> list2 = new ArrayList<>();
        list2.add(A.maps("num", "G1082-AUTOTEST9336290", "pcs", "0"));
        list2.add(A.maps("num", "G1082-AUTOTEST9336291", "pcs", "1"));
        data.put("dt", list2);

        String file = "/home/ty/test-number.pdf";
        PdfUtil.generatePdfFile(file, print, data);
    }
}
