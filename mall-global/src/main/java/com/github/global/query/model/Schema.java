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
public class Schema {

    /** 表名 */
    private String name;

    /** 表说明 */
    private String desc;

    /** 表别名 */
    private String alias;

    /** 列信息 */
    private Map<String, SchemaColumn> columnMap;

    /** 主键列 */
    private List<String> idKey;

    public Schema(String name, String desc, String alias, Map<String, SchemaColumn> columnMap) {
        this.name = name;
        this.desc = desc;
        this.alias = alias;
        this.columnMap = columnMap;

        List<String> idKey = new ArrayList<>();
        if (!columnMap.isEmpty()) {
            for (SchemaColumn schemaColumn : columnMap.values()) {
                if (schemaColumn.isPrimary()) {
                    idKey.add(schemaColumn.getName());
                }
            }
        }
        this.idKey = idKey;
    }

    public String idKeyColumn(boolean needAlias, String alias) {
        if (idKey.size() == 1) {
            if (needAlias) {
                return QuerySqlUtil.toSqlField(alias) + "." + QuerySqlUtil.toSqlField(idKey.get(0));
            } else {
                return QuerySqlUtil.toSqlField(idKey.get(0));
            }
        } else {
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (String id : idKey) {
                if (needAlias) {
                    sj.add(QuerySqlUtil.toSqlField(alias) + "." + QuerySqlUtil.toSqlField(id));
                } else {
                    sj.add(QuerySqlUtil.toSqlField(id));
                }
            }
            return sj.toString();
        }
    }
}
