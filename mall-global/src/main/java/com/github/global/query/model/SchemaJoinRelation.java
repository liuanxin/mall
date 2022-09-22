package com.github.global.query.model;

import com.github.global.query.enums.JoinType;
import com.github.global.query.util.QuerySqlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchemaJoinRelation {

    private Schema masterSchema;
    private JoinType joinType;
    private Schema childSchema;

    public String generateJoin(SchemaColumnInfo scInfo) {
        String masterSchemaName = masterSchema.getName();
        String childSchemaName = childSchema.getName();
        SchemaColumnRelation relation = scInfo.findRelationByMasterChild(masterSchemaName, childSchemaName);
        String masterAlias = masterSchema.getAlias();
        String childAlias = QuerySqlUtil.toSqlField(childSchema.getAlias());
        return " " + joinType.name() +
                " JOIN " + QuerySqlUtil.toSqlField(childSchemaName) +
                " AS " + childAlias + " ON " + childAlias +
                "." + QuerySqlUtil.toSqlField(relation.getOneOrManyColumn()) +
                " = " + QuerySqlUtil.toSqlField(masterAlias) +
                "." + QuerySqlUtil.toSqlField(relation.getOneColumn());
    }
}
