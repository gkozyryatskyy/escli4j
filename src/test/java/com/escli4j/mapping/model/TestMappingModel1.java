package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test", type = "test")
public class TestMappingModel1 {

    @Field(dataType = DataType.TEXT)
    public String field1;
    @Field(dataType = DataType.KEYWORD)
    public String field2;
    @Field(dataType = DataType.OBJECT)
    public Inner field3;
    @Field(dataType = DataType.KEYWORD, fields = { @InnerField(name = "indexes", dataType = DataType.TEXT) })
    public String field4;

    public static class Inner {
        @Field(dataType = DataType.TEXT)
        public Inner field5;
        @Field(dataType = DataType.KEYWORD)
        public Inner field6;
    }

}
