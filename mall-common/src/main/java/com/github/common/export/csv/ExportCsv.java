package com.github.common.export.csv;

import com.github.common.util.A;
import com.github.common.util.U;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class ExportCsv {

    // csv 用英文逗号(,)隔开列, 用换行(\n)隔开行, 内容中包含了逗号的需要用双引号包裹, 若内容中包含了双引号则需要用两个双引号表示

    private static final String SPLIT = ",";
    private static final String WRAP = "\n";

    private static final String QUOTE = "\"";
    private static final String REPLACE_QUOTE = "\"\"";

    public static String getContent(LinkedHashMap<String, String> titleMap, List<?> dataList) {
        StringBuilder sbd = new StringBuilder();
        if (A.isNotEmpty(titleMap)) {
            int i = 0;
            for (String title : titleMap.values()) {
                sbd.append(handleCsvContent(title));
                i++;
                if (i != titleMap.size()) {
                    sbd.append(SPLIT);
                }
            }
            if (A.isNotEmpty(dataList)) {
                Set<String> titles = titleMap.keySet();
                for (Object data : dataList) {
                    if (sbd.length() > 0) {
                        sbd.append(WRAP);
                    }
                    i = 0;
                    for (String title : titles) {
                        sbd.append(handleCsvContent(U.getField(data, title)));
                        i++;
                        if (i != titleMap.size()) {
                            sbd.append(SPLIT);
                        }
                    }
                }
            }
        }
        return sbd.toString();
    }

    private static String handleCsvContent(String content) {
        if (content.contains(QUOTE)) {
            content = content.replace(QUOTE, REPLACE_QUOTE);
        }
        if (content.contains(SPLIT)) {
            content = QUOTE + content + QUOTE;
        }
        return content;
    }
}
