package com.github;

import com.github.common.json.JsonUtil;
import lombok.Data;

public class Nil {

//    public static void main(String[] args) {
//        System.out.println("".getBytes(StandardCharsets.UTF_8).length);
//        System.out.println("abc".getBytes(StandardCharsets.UTF_8).length);
//        System.out.println("abc 中国".getBytes(StandardCharsets.UTF_8).length);
//        System.out.println("中国".getBytes(StandardCharsets.UTF_8).length);
//    }

//    public static void main(String[] args) {
//        /*
//        String sql = "update order_product op left join orders o on op.order_code = o.order_code set op.customer_code = o.customer_code " +
//                "where o.create_time >= '%s' and o.create_time <= '%s';\n";
//        Date min = DateUtil.parse("2015-07-15");
//        Date max = DateUtil.now();
//
//        int day = DateUtil.betweenDay(min, max);
//        for (int i = 0; i < day; i++) {
//            Date date = DateUtil.addDays(min, i);
//            String start = DateUtil.format(DateUtil.getDayStart(date), DateFormatType.YYYY_MM_DD);
//            String end = DateUtil.format(DateUtil.getDayEnd(date), DateFormatType.YYYY_MM_DD_HH_MM_SSSSS);
//            System.out.printf(sql, start, end);
//        }
//        */
//
//
//        String sql = "update order_operation_time opt left join orders o on opt.order_code = o.order_code set opt.customer_code = o.customer_code " +
//                "where o.create_time >= '%s' and o.create_time <= '%s';\n";
//        Date min = DateUtil.parse("2015-07-15");
//        Date max = DateUtil.now();
//
//        int day = DateUtil.betweenDay(min, max);
//        for (int i = 0; i < day; i++) {
//            Date date = DateUtil.addDays(min, i);
//            String start = DateUtil.format(DateUtil.getDayStart(date), DateFormatType.YYYY_MM_DD);
//            String end = DateUtil.format(DateUtil.getDayEnd(date), DateFormatType.YYYY_MM_DD_HH_MM_SSSSS);
//            System.out.printf(sql, start, end);
//        }
//    }



//    public static void main(String[] args) throws IOException {
//        HttpServletResponse response = null;
//        List<List<String>> head = new ArrayList<>();
//        head.add(Collections.singletonList("head1"));
//        head.add(Collections.singletonList("head2"));
//        head.add(Collections.singletonList("head3"));
//        head.add(Collections.singletonList("head4"));
//
//        List<List<Object>> data = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            data.add(Arrays.asList("abc\n字符串" + i, new Date(), 0.56, (i % 2 == 0)));
//        }
//
//        ExcelWriter excelWriter = null;
//        try {
//            excelWriter = EasyExcel.write("/home/ty/a.xlsx").excelType(ExcelTypeEnum.XLSX).useDefaultStyle(false).build();
//
//            excelWriter.write(data, EasyExcel.writerSheet("demoData-1").head(head)
//                    .registerWriteHandler(new FreezeTitleSheetHandler())
//                    .registerWriteHandler(new StyleCellHandler())
//                    .build());
//            excelWriter.write(data, EasyExcel.writerSheet("demoData-2").head(head).build());
//        } finally {
//            if (excelWriter != null) {
//                excelWriter.finish();
//            }
//        }
//        System.out.println("成功");

//        String url = "https://oapi.dingtalk.com/robot/send?access_token=43cd62097f33c019ea3cfc081cba4fdfbbac3d1beb8a172b1a146b6b2cb9393f";
//        String content = "时间区间(2020-01-01 10:20:00 ~ 2020-01-01 11:20:00)\n" +
//                "- 数据库表(t_user)记录数: (10), es 索引(user)记录数: (20)\n" +
//                "- 数据库表(t_xxxx)记录数: (80), es 索引(xxxx)记录数: (90)\n";
//        HttpClientUtil.postBody(url, JsonUtil.toJson(A.maps(
//                "msgtype", "markdown",
//                "markdown", A.maps(
//                        "title", "数据库与 es 索引数量不一致",
//                        "text", "监控报警(测试): " + content
//                )
//        )));
//    }



//    static String alias = "41325737";
//
//    @SuppressWarnings("rawtypes")
//    public static void main(String[] args) {
//        long prefix = U.toLong(alias.trim()) + 192168;
//        String html = HttpClientUtil.get("https://shop" + prefix + ".m.youzan.com/wscshop/showcase/homepage?kdt_id=" + alias);
//        if (U.isEmpty(html)) {
//            System.out.println("响应为空!");
//            return;
//        }
//
//        String script = Jsoup.parse(html).getElementsByTag("script").get(1).toString();
//        String s1 = "window._global = ";
//        String s2 = "if (_global.url && _global.kdt_id) {";
//        String json = script.substring(script.indexOf(s1) + s1.length(), script.indexOf(s2));
//        Map map = (Map) JsonUtil.toObject(json, Map.class).get("mp_data");
//
//        String name = U.toStr(map.get("shop_name"));
//        String desc = U.toStr(map.get("intro"));
//        String image = U.toStr(map.get("logo"));
//        if (U.isEmpty(name) || U.isEmpty(image)) {
//            System.out.println("名称或图片为空");
//            return;
//        }
//
//        HttpClientUtil.post("https://api-boss.smmdhao.com/admin/youzan", A.maps(
//                "token", "ded2e36f0cbe4cfc",
//
//                "name", name.trim(),
//                "description", desc.trim(),
//                "homeUrl", "https://shop" + prefix + ".youzan.com/wscshop/feature/goods/all?kdt_id=" + alias.trim(),
//                "image", image.contains("?") ? image.substring(0, image.indexOf("?")).trim() : image
//        ));
//    }

    @Data
    public static class Abc {
        private String abc;
    }


    public static void main(String[] args) {
        String json = "{\"abc\":123}";
        Abc abc = JsonUtil.toObject(json, Abc.class);
        System.out.println(abc);
    }
}
