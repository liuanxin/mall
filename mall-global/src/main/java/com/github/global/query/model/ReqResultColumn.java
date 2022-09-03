package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqResultColumn {

    private ReqResultGroupFunction function;
    private String column;

    public String generateSql(String defaultScheme, Map<String, Scheme> schemeMap) {
        String checkType = "group(" + function.name().toLowerCase() + ")";
        String alias;
        if (function == ReqResultGroupFunction.COUNT) {
            if (!Set.of("*", "1", "0").contains(column)) {
                QueryUtil.checkColumnName(column, defaultScheme, schemeMap, checkType);
            }
            alias = QueryConst.COUNT_ALIAS;
        } else {
            alias = QueryUtil.checkColumnName(column, defaultScheme, schemeMap, checkType).getName();
        }
        return String.format(function.getValue(), column) + " AS " + alias;
    }
}
