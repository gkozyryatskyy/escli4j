package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.mapping.DataType;

public class SubMappingModel1 extends ParrentMappingModel1 {

    @Field(dataType = DataType.STRING)
    public String subField1;
    @Field(dataType = DataType.STRING)
    public String subField2;
    @Field(dataType = DataType.STRING)
    public String subField3;
    public String subField4;
    
}
