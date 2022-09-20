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
        String masterTableName = masterTable.getName();
        String childTableName = childTable.getName();
        TableColumnRelation relation = tableColumnInfo.findRelationByMasterChild(masterTableName, childTableName);
        String masterAlias = masterTable.getAlias();
        String childAlias = childTable.getAlias();
        return " " + joinType.name() +
                " JOIN " + QuerySqlUtil.toSqlField(childTableName) +
                " AS " + QuerySqlUtil.toSqlField(childAlias) +
                " ON " + QuerySqlUtil.toSqlField(childAlias) +
                "." + QuerySqlUtil.toSqlField(relation.getOneOrManyColumn()) +
                " = " + QuerySqlUtil.toSqlField(masterAlias) +
                "." + QuerySqlUtil.toSqlField(relation.getOneColumn());
    }
}
