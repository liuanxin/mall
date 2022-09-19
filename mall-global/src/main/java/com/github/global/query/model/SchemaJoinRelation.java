package com.github.global.query.model;

import com.github.global.query.enums.ReqJoinType;
import com.github.global.query.util.QuerySqlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchemaJoinRelation {

    private Schema masterSchema;
    private ReqJoinType joinType;
    private Schema childSchema;

    public String generateJoin(SchemaColumnInfo schemaColumnInfo) {
        StringBuilder sbd = new StringBuilder();
        String masterSchemaName = masterSchema.getName();
        String childSchemaName = childSchema.getName();
        SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(masterSchemaName, childSchemaName);
        if (relation != null) {
            String masterAlias = masterSchema.getAlias();
            String childAlias = childSchema.getAlias();

            sbd.append(" ").append(joinType.name());
            sbd.append(" JOIN ").append(QuerySqlUtil.toSqlField(childSchemaName));
            sbd.append(" AS ").append(QuerySqlUtil.toSqlField(childAlias));
            sbd.append(" ON ").append(QuerySqlUtil.toSqlField(masterSchemaName));
            sbd.append(".").append(QuerySqlUtil.toSqlField(masterAlias));
            sbd.append(" = ").append(QuerySqlUtil.toSqlField(childAlias));
            sbd.append(".").append(QuerySqlUtil.toSqlField(relation.getOneOrManyColumn()));
        }
        return sbd.toString();
    }
}
