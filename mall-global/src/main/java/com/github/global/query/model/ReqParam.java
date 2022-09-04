package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.global.query.util.QueryUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * {
 *   "query": ...
 *   "sort": { "createTime": "desc", "id", "asc" },
 *   "page": [ 1, 20 ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParam {

    private static final Integer MIN_LIMIT = 20;
    private static final Set<Integer> LIMIT_SET = new HashSet<>(Arrays.asList(
            MIN_LIMIT, 50, 100, 200, 500, 1000)
    );


    /** 查询信息 */
    private ReqParamOperate query;

    /** 排序信息 */
    private Map<String, String> sort;

    /** 分页信息 */
    private List<Integer> page;


    public void checkParam(String mainScheme, TableColumnInfo columnInfo) {
        if (query == null) {
            throw new RuntimeException("no query param");
        }
        query.checkCondition(mainScheme, columnInfo);

        if (sort != null && !sort.isEmpty()) {
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String column = entry.getKey();
                QueryUtil.checkColumnName(column, mainScheme, columnInfo, "query order");
            }
        }
    }

    public String generateOrderSql(String mainScheme, Map<String, Scheme> schemeMap) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                orderSj.add(entry.getKey() + " " + ("asc".equalsIgnoreCase(entry.getValue()) ? "ASC" : "DESC"));
            }
            String orderBy = orderSj.toString();
            if (!orderBy.isEmpty()) {
                return " ORDER BY " + orderBy;
            }
        }
        return "";
    }

    private String generatePageSql(List<Object> params) {
        if (page != null && !page.isEmpty()) {
            int indexParam = page.get(0);
            int index = indexParam <= 0 ? 1 : indexParam;

            int limitParam = page.size() > 1 ? page.get(1) : 0;
            int limit = LIMIT_SET.contains(limitParam) ? limitParam : MIN_LIMIT;

            if (index == 1) {
                params.add(limit);
                return " LIMIT ?";
            } else {
                params.add((index - 1) * limit);
                params.add(limit);
                return " LIMIT ?, ?";
            }
        }
        return "";
    }
}
