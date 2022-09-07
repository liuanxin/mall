package com.github.global.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaColumnRelation {

    private String oneSchema;
    private String oneColumn;
    private SchemaRelationType type;
    private String oneOrManySchema;
    private String oneOrManyColumn;
}
