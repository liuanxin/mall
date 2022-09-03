package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqResult {

    /** 结构 */
    private String scheme;
    /** 结构里的属性 */
    private List<String> columns;
    /** 函数 */
    private List<List<String>> functions;

    /** 结构里的关系 */
    private Map<String, ReqResult> relations;


    public String generateFunctionSql(Map<String, Scheme> schemeMap) {
        if (functions != null && !functions.isEmpty()) {
            StringJoiner sj = new StringJoiner(", ");
            for (List<String> groups : functions) {
                if (groups != null && !groups.isEmpty()) {
                    ReqResultGroup group = ReqResultGroup.deserializer(groups.get(0));
                    String column = groups.size() > 1 ? groups.get(1) : "";

                    String checkType = "group(" + group.name().toLowerCase() + ")";
                    String alias;
                    if (group == ReqResultGroup.COUNT) {
                        if (!Set.of("*", "1").contains(column)) {
                            QueryUtil.checkColumnName(column, scheme, schemeMap, checkType);
                        } else if (column.isEmpty()) {
                            column = "*";
                        }
                        alias = QueryConst.COUNT_ALIAS;
                    } else {
                        alias = QueryUtil.checkColumnName(column, scheme, schemeMap, checkType).getName();
                    }
                    sj.add(String.format(group.getValue(), column) + " AS " + alias);
                }
            }
            String groupBy = sj.toString();
            if (groupBy.isEmpty()) {
                return " GROUP BY " + groupBy;
            }
        }
        return "";
    }
}
