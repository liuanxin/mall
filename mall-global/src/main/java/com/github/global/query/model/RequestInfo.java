package com.github.global.query.model;

import com.github.global.query.enums.ReqResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfo {

    /** 主结构 */
    private String table;
    /** 入参 */
    private ReqParam param;
    /** 出参的结构类型, 对象(obj)还是数组(arr), 不设置则默认是数组 */
    private ReqResultType type;
    /** 出参 */
    private ReqResult result;


    public Set<String> checkParam(TableColumnInfo tableColumnInfo) {
        if (table == null || table.isEmpty()) {
            throw new RuntimeException("no table");
        }
        if (tableColumnInfo.findTable(table) == null) {
            throw new RuntimeException("no table(" + table + ") defined");
        }
        if (param == null) {
            throw new RuntimeException("request need param");
        }
        return param.checkParam(table, tableColumnInfo);
    }

    public Set<String> checkResult(TableColumnInfo tableColumnInfo) {
        if (result == null) {
            throw new RuntimeException("request need result");
        }
        return result.checkResult(table, tableColumnInfo);
    }
}
