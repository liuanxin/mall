package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfo {

    /** 主结构, 如果入参和出参是相同的, 只需要在这里定义一次就可以了 */
    private String scheme;

    /** 入参 */
    private ReqParam param;

    /** 出参 */
    private ReqResult result;


    public void check(TableColumnInfo columnInfo) {
        if (scheme != null && !scheme.isEmpty()) {
            Map<String, Scheme> schemeMap = columnInfo.getSchemeMap();
            if (!schemeMap.containsKey(scheme)) {
                throw new RuntimeException("no scheme(" + scheme + ") defined");
            }
        }

        if (param == null) {
            throw new RuntimeException("request need param");
        }
        param.checkParam(scheme, columnInfo);

        if (result == null) {
            throw new RuntimeException("request need result");
        }
        result.checkResult(scheme, columnInfo);
    }
}
