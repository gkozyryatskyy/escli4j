package com.escli4j.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test", type = "TestModel1")
public class TestModel1 extends EsEntity {

    private static final long serialVersionUID = 6359742874223042889L;
    
    @Field(dataType = DataType.TEXT)
    public String field1;

}
