package com.github.global.query.model;

import com.github.global.query.util.QuerySqlUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Data
@NoArgsConstructor
public class Table {

    /** 表名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 表别名 */
    private String alias;

    /** 列信息 */
    private Map<String, TableColumn> columnMap;

    /** 主键列 */
    private List<String> idKey;

    public Table(String name, String desc, String alias, Map<String, TableColumn> columnMap) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.columnMap = columnMap;

        List<String> idKey = new ArrayList<>();
        if (!columnMap.isEmpty()) {
            for (TableColumn tableColumn : columnMap.values()) {
                if (tableColumn.isPrimary()) {
                    idKey.add(tableColumn.getName());
                }
            }
        }
        this.idKey = idKey;
    }

    public String idWhere(boolean needAlias) {
        if (idKey.size() == 1) {
            String column = QuerySqlUtil.toSqlField(idKey.get(0));
            if (needAlias) {
                return QuerySqlUtil.toSqlField(alias) + "." + column;
            } else {
                return column;
            }
        } else {
            return "(" + idSelect(needAlias) + ")";
        }
    }
    public String idSelect(boolean needAlias) {
        StringJoiner sj = new StringJoiner(", ");
        for (String id : idKey) {
            String column = QuerySqlUtil.toSqlField(id);
            if (needAlias) {
                sj.add(QuerySqlUtil.toSqlField(alias) + "." + column);
            } else {
                sj.add(column);
            }
        }
        return sj.toString();
    }
}
