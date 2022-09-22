package com.github.global.query.model;

import com.github.global.query.enums.SchemaRelationType;
import lombok.Data;

import java.util.Objects;

@Data
public class SchemaColumnRelation {

    private String oneSchema;
    private String oneColumn;
    private SchemaRelationType type;
    private String oneOrManySchema;
    private String oneOrManyColumn;

    public SchemaColumnRelation(String oneSchema, String oneColumn, SchemaRelationType type, String oneOrManySchema, String oneOrManyColumn) {
        this.oneSchema = oneSchema;
        this.oneColumn = oneColumn;
        this.type = type;
        this.oneOrManySchema = oneOrManySchema;
        this.oneOrManyColumn = oneOrManyColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SchemaColumnRelation that = (SchemaColumnRelation) o;
        return oneSchema.equals(that.oneSchema) && oneColumn.equals(that.oneColumn)
                && oneOrManySchema.equals(that.oneOrManySchema) && oneOrManyColumn.equals(that.oneOrManyColumn);
    }
    @Override
    public int hashCode() {
        return Objects.hash(oneSchema, oneColumn, oneOrManySchema, oneOrManyColumn);
    }
}
