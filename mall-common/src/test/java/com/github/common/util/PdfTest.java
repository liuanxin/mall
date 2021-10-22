package com.github.common.util;

import com.github.common.pdf.PdfUtil;
import com.github.common.pdf.PrintInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.itextpdf.text.Element;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

public class PdfTest {

    @Test
    public void generate() {
        PrintInfo print = new PrintInfo();
        // print.setWidth(595F);
        // print.setHeight(842F);

        PrintInfo.WatermarkInfo watermark = new PrintInfo.WatermarkInfo();
        watermark.setValue("这里有水印");
        print.setWatermark(watermark);

        List<PrintInfo.DataContent> holdContentList = Lists.newArrayList();
        PrintInfo.DataContent phc0 = new PrintInfo.DataContent();
        phc0.setX(210F);
        phc0.setY(800F);
        phc0.setValue("Packing List");
        phc0.setFontSize(18F);
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

        List<PrintInfo.TableInfo> tableList = Lists.newArrayList();
        PrintInfo.TableInfo pt1 = new PrintInfo.TableInfo();
        PrintInfo.TableHead key1 = new PrintInfo.TableHead();
        key1.setX(306);
        key1.setY(768);
        key1.setBorder(false);
        key1.setPrintHead(false);
        key1.setFieldWidthList(Arrays.asList(60F, 180F));
        key1.setFieldName("orderInfo");
        pt1.setKey(key1);

        List<PrintInfo.TableContent> value1 = Lists.newArrayList();
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
        key2.setY(80);
        key2.setPrintHead(false);
        key2.setFieldWidthList(Arrays.asList(155F, 90F, 120F, 155F));
        key2.setFieldName("printInfo");
        pt2.setKey(key2);

        List<PrintInfo.TableContent> value2 = Lists.newArrayList();
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
        dynamicTableKey.setFieldWidthList(Arrays.asList(/*40F,*/ 60F, 78F, 55F, 55F, 53F, 45F, 105F, 68F));
        dynamicTableKey.setHeadList(Arrays.asList(/*"序号", */"分拣码*箱数", "SKU", "预报总数量", "实收总数量", "SKU总箱数", "实收箱数", "单品规格(CM)", "单品重量(KG)"));
        dynamicTableKey.setBackRgba(Arrays.asList(220, 220, 220));
        dynamicTableKey.setTextAlign(Element.ALIGN_CENTER);
        dynamicTableKey.setFontSize(10F);
        dynamicTableKey.setFontBold(true);
        dynamicTableKey.setBorder(true);
        dynamicTableKey.setHeight(24);
        dynamicTableKey.setSinglePageCount(15);
        print.setDynamicHead(dynamicTableKey);

        List<PrintInfo.TableContent> dynamicTableValue = Lists.newArrayList();
        // PrintInfo.TableContent dptc0 = new PrintInfo.TableContent();
        // dptc0.setFieldType(PrintInfo.PlaceholderType.TABLE_LINE_INDEX);
        // dptc0.setTextAlign(Element.ALIGN_CENTER);
        // dynamicTableValue.add(dptc0);

        PrintInfo.TableContent dptc1 = new PrintInfo.TableContent();
        dptc1.setFieldName("stockCodeAndBoxNum");
        dptc1.setTextAlign(Element.ALIGN_CENTER);
        dptc1.setHeight(28);
        dptc1.setFontSize(8F);
        dynamicTableValue.add(dptc1);

        PrintInfo.TableContent dptc2 = new PrintInfo.TableContent();
        dptc2.setFieldName("productCode");
        dptc2.setTextAlign(Element.ALIGN_CENTER);
        dptc2.setHeight(28);
        dptc2.setFontSize(8F);
        dynamicTableValue.add(dptc2);

        PrintInfo.TableContent dptc3 = new PrintInfo.TableContent();
        dptc3.setFieldName("forecastNum");
        dptc3.setTextAlign(Element.ALIGN_CENTER);
        dptc3.setHeight(28);
        dptc3.setFontSize(8F);
        dynamicTableValue.add(dptc3);

        PrintInfo.TableContent dptc4 = new PrintInfo.TableContent();
        dptc4.setFieldName("actualNum");
        dptc4.setTextAlign(Element.ALIGN_CENTER);
        dptc4.setHeight(28);
        dptc4.setFontSize(8F);
        dynamicTableValue.add(dptc4);

        PrintInfo.TableContent dptc5 = new PrintInfo.TableContent();
        dptc5.setFieldName("skuNum");
        dptc5.setTextAlign(Element.ALIGN_CENTER);
        dptc5.setHeight(28);
        dptc5.setFontSize(8F);
        dynamicTableValue.add(dptc5);

        PrintInfo.TableContent dptc6 = new PrintInfo.TableContent();
        dptc6.setFieldName("actualSkuNum");
        dptc6.setTextAlign(Element.ALIGN_CENTER);
        dptc6.setHeight(28);
        dptc6.setFontSize(8F);
        dynamicTableValue.add(dptc6);

        PrintInfo.TableContent dptc7 = new PrintInfo.TableContent();
        dptc7.setFieldName("size");
        dptc7.setTextAlign(Element.ALIGN_CENTER);
        dptc7.setHeight(28);
        dptc7.setFontSize(8F);
        dynamicTableValue.add(dptc7);

        PrintInfo.TableContent dptc8 = new PrintInfo.TableContent();
        dptc8.setFieldName("weight");
        dptc8.setTextAlign(Element.ALIGN_CENTER);
        dptc8.setHeight(28);
        dptc8.setFontSize(8F);
        dynamicTableValue.add(dptc8);
        print.setDynamicContent(dynamicTableValue);

        Map<String, Object> data = Maps.newHashMap();
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
        List<Map<Object, Object>> list = Lists.newArrayList();
        for (int i = 0; i < 31; i++) {
            list.add(A.maps(
                            "stockCodeAndBoxNum", "0*1",
                            "productCode", "00014-A4B01",
                            "forecastNum", "40",
                            "actualNum", "40",
                            "skuNum", "1",
                            "actualSkuNum", "1",
                            "size", "16.00 * 16.00 * 4.50",
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
                    "size", "16.00 * 16.00 * 4.50",
                    "weight", "0.270"
            ));
        }
        data.put("productList", list);

        // System.out.println("===================");
        // System.out.println(JsonUtil.toJson(print)); // 模板
        // System.out.println("===================");
        // System.out.println(JsonUtil.toJson(data)); // 数据
        // System.out.println("===================");

        String file = "/home/ty/list-test.pdf";
        PdfUtil.generatePdfFile(file, print, data);
        System.out.printf("生成文件 %s 成功\n", file);

        // String encode = Encrypt.base64Encode(PdfUtil.generatePdfByte(print, data));
        // System.out.println(encode);
        // System.out.println(encode.length());

        // String compress = U.compress(encode);
        // System.out.println(compress);
        // System.out.println(compress.length());
    }
}
