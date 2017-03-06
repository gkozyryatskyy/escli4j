package com.escli4j.mapping.model;

import java.util.List;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;
import com.escli4j.model.EsEntity;

@Type(index = "test2", type = "test2")
public class TestMappingModel2 extends EsEntity {

    private static final long serialVersionUID = 320566017090586743L;
    
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
