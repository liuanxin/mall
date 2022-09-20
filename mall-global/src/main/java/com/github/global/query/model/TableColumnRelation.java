package com.github.global.query.model;

import com.github.global.query.enums.TableRelationType;
import lombok.Data;

import java.util.Objects;

@Data
public class TableColumnRelation {

    private String oneTable;
    private String oneColumn;
    private TableRelationType type;
    private String oneOrManyTable;
    private String oneOrManyColumn;

    public TableColumnRelation(String oneTable, String oneColumn, TableRelationType type, String oneOrManyTable, String oneOrManyColumn) {
        this.oneTable = oneTable;
        this.oneColumn = oneColumn;
        this.type = type;
        this.oneOrManyTable = oneOrManyTable;
        this.oneOrManyColumn = oneOrManyColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        TableColumnRelation that = (TableColumnRelation) o;
        return oneTable.equals(that.oneTable) && oneColumn.equals(that.oneColumn)
                && oneOrManyTable.equals(that.oneOrManyTable) && oneOrManyColumn.equals(that.oneOrManyColumn);
    }
    @Override
    public int hashCode() {
        return Objects.hash(oneTable, oneColumn, oneOrManyTable, oneOrManyColumn);
    }
}
