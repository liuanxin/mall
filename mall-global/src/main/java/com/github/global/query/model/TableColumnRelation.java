package com.github.global.query.model;

import com.github.global.query.annotation.RelationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableColumnRelation {

    private RelationType type;
    private String tableColumn;
}
