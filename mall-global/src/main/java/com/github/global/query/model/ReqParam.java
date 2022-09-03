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
// *   "group": [ "sku", "" ]
 *   "sort": { "createTime": "desc", "id", "asc" },
 *   "page": [ 1, 20 ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReqParam {

    private static final Set<Integer> LIMIT_SET = new LinkedHashSet<>(Arrays.asList(20, 50, 100, 500, 1000));
    private static final Integer MIN_LIMIT = LIMIT_SET.iterator().next();

    private ReqParamOperate query;

//    private List<String> group;

    private Map<String, String> sort;

    private List<Integer> page;

//    public String generateGroupSql(String defaultScheme, Map<String, Scheme> schemeMap) {
//        if (group != null && !group.isEmpty()) {
//            StringJoiner groupSj = new StringJoiner(", ");
//            for (String column : group) {
//                QueryUtil.checkColumnName(column, defaultScheme, schemeMap, "group");
//                groupSj.add(column);
//            }
//            if (!groupSj.toString().isEmpty()) {
//                return " GROUP BY " + groupSj;
//            }
//        }
//        return "";
//    }

    public String generateOrderSql(String defaultScheme, Map<String, Scheme> schemeMap) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String column = entry.getKey();
                QueryUtil.checkColumnName(column, defaultScheme, schemeMap, "order");
                orderSj.add(column + " " + ("asc".equalsIgnoreCase(entry.getValue()) ? "ASC" : "DESC"));
            }
            if (!orderSj.toString().isEmpty()) {
                return " ORDER BY " + orderSj;
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
