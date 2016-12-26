package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test", type = "testChild", parent = "test")
public class TestMappingModel3 {
    
    @Field(dataType = DataType.STRING)
    public String field1;

}
