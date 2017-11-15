package com.escli4j.mapping.model;

import java.util.List;
import java.util.Set;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;
import com.escli4j.model.EsEntity;

@Type(index = "test2", type = "test2")
public class TestMappingModel2 extends EsEntity {
    
    @Field(dataType = DataType.TEXT)
    public String field1;
    @Field(dataType = DataType.NESTED)
    public Inner[] field2;
    @Field(dataType = DataType.NESTED)
    public List<Inner> field3;
    @Field(dataType = DataType.NESTED)
    public Set<Inner> field4;

    public static class Inner {
        @Field(dataType = DataType.KEYWORD, fields = { @InnerField(dataType = DataType.TEXT, name = "indexed") })
        public Inner field5;
    }

}
