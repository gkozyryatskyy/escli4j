package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.mapping.DataType;

public class ParrentMappingModel2 {

    @Field(dataType = DataType.TEXT)
    public String parrentField1;
    @Field(dataType = DataType.TEXT)
    public String parrentField2;
    @Field(dataType = DataType.TEXT)
    public String parrentField3;
    public String parrentField4;
}
