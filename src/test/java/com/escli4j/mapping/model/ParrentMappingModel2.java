package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.mapping.DataType;
import com.escli4j.model.EsEntity;

public class ParrentMappingModel2 extends EsEntity {

    private static final long serialVersionUID = 5086766368725245529L;
    
    @Field(dataType = DataType.TEXT)
    public String parrentField1;
    @Field(dataType = DataType.TEXT)
    public String parrentField2;
    @Field(dataType = DataType.TEXT)
    public String parrentField3;
    public String parrentField4;
}
