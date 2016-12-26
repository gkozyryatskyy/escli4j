package com.escli4j.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test", type = "test")
public class TestModel1 {

    @Field(dataType = DataType.STRING)
    public String field1;

}
