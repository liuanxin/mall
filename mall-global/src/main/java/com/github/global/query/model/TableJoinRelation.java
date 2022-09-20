package com.github.global.query.model;

import com.github.global.query.enums.ReqJoinType;
import com.github.global.query.util.QuerySqlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableJoinRelation {

    private Table masterTable;
    private ReqJoinType joinType;
    private Table childTable;

    public String generateJoin(TableColumnInfo tableColumnInfo) {
        StringBuilder sbd = new StringBuilder();
        String masterTableName = masterTable.getName();
        String childTableName = childTable.getName();
        TableColumnRelation relation = tableColumnInfo.findRelationByMasterChild(masterTableName, childTableName);
        if (relation != null) {
            String masterAlias = masterTable.getAlias();
            String childAlias = childTable.getAlias();

            sbd.append(" ").append(joinType.name());
            sbd.append(" JOIN ").append(QuerySqlUtil.toSqlField(childTableName));
            sbd.append(" AS ").append(QuerySqlUtil.toSqlField(childAlias));
            sbd.append(" ON ").append(QuerySqlUtil.toSqlField(masterTableName));
            sbd.append(".").append(QuerySqlUtil.toSqlField(masterAlias));
            sbd.append(" = ").append(QuerySqlUtil.toSqlField(childAlias));
            sbd.append(".").append(QuerySqlUtil.toSqlField(relation.getOneOrManyColumn()));
        }
        return sbd.toString();
    }
}
