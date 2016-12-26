package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test", type = "test")
public class SubMappingModel2 extends ParrentMappingModel2 {

    @Field(dataType = DataType.STRING)
    public String subField1;
    @Field(dataType = DataType.STRING)
    public String subField2;
    @Field(dataType = DataType.STRING)
    public String subField3;
    public String subField4;
}
