package com.github.global.query.model;

import com.github.global.query.enums.SchemaRelationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaColumnRelation {

    private String oneSchema;
    private String oneColumn;
    private SchemaRelationType type;
    private String oneOrManySchema;
    private String oneOrManyColumn;

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
