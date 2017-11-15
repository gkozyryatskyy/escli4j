package com.escli4j.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test", type = "TestModel1")
public class TestModel1 extends EsEntity {
    
    @Field(dataType = DataType.TEXT)
    public String field1;

}
