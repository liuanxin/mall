package com.github.global.query.model;

import com.github.global.query.util.QueryUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        if (!columnMap.isEmpty()) {
            List<String> idKey = new ArrayList<>();
            for (SchemaColumn schemaColumn : columnMap.values()) {
                if (schemaColumn.isPrimary()) {
                    idKey.add(QueryUtil.defaultIfBlank(schemaColumn.getAlias(), schemaColumn.getName()));
                }
            }
            this.idKey = idKey;
        }
    }
}
