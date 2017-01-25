package com.escli4j.mapping.model;

import java.util.List;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test", type = "test")
public class TestMappingModel2 {

    @Field(dataType = DataType.TEXT)
    public String field1;
    @Field(dataType = DataType.NESTED)
    public Inner[] field2;
    @Field(dataType = DataType.NESTED)
    public List<Inner> field3;

    public static class Inner {
        @Field(dataType = DataType.KEYWORD, fields = { @InnerField(dataType = DataType.TEXT, name = "indexed") })
        public Inner field4;
    }

}
