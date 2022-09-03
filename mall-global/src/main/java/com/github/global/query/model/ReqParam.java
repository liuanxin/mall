package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParam {

    private static final Set<Integer> LIMIT_SET = new LinkedHashSet<>(Arrays.asList(20, 50, 100, 500, 1000));
    private static final Integer MIN_LIMIT = LIMIT_SET.iterator().next();

    private ReqParamOperate query;

    /** { "createTime": "desc", "id", "asc" } */
    private Map<String, String> order;

    private Integer page;
    private Integer limit;

    public String checkOrder(String defaultScheme, Map<String, Scheme> schemeMap) {
        if (order != null && !order.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : order.entrySet()) {
                String column = entry.getKey();
                String schemeName, columnName;
                if (column.contains(".")) {
                    String[] arr = column.split("\\.");
                    schemeName = arr[0].trim();
                    columnName = arr[1].trim();
                } else {
                    schemeName = defaultScheme;
                    columnName = column;
                }

                Scheme scheme = schemeMap.get(schemeName);
                if (scheme == null) {
                    throw new RuntimeException("no scheme(" + schemeName + ") in order");
                }
                SchemeColumn schemeColumn = scheme.getColumnMap().get(columnName);
                if (schemeColumn == null) {
                    throw new RuntimeException("scheme(" + schemeName + ") no column(" + columnName + ") in order");
                }

                orderSj.add(column + " " + ("asc".equalsIgnoreCase(entry.getValue()) ? "ASC" : "DESC"));
            }
            if (!orderSj.toString().isEmpty()) {
                return " ORDER BY " + orderSj;
            }
        }
        return "";
    }

    private String checkPage() {
        if (page != null && page <= 0) {
            page = 1;
        }
        limit = LIMIT_SET.contains(limit) ? limit : MIN_LIMIT;
        if (page != null) {
            return " LIMIT " + (page == 1 ? limit : (((page - 1) * limit) + ", " + limit));
        }
        return "";
    }

    public boolean needPage() {
        return page != null && limit != null;
    }
}
