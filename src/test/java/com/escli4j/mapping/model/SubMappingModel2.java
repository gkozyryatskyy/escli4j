package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;

@Type(index = "test_sub2", type = "test_sub2")
public class SubMappingModel2 extends ParrentMappingModel2 {

    private static final long serialVersionUID = -7226475228880644698L;
    
    @Field(dataType = DataType.TEXT)
    public String subField1;
    @Field(dataType = DataType.TEXT)
    public String subField2;
    @Field(dataType = DataType.TEXT)
    public String subField3;
    public String subField4;
}
