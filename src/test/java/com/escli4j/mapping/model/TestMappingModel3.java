package com.escli4j.mapping.model;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;
import com.escli4j.model.EsChildEntity;

@Type(index = "test1", type = "test_child3", parent = "test1")
public class TestMappingModel3 extends EsChildEntity {
    
    private static final long serialVersionUID = -2275602955102780427L;
    
    @Field(dataType = DataType.TEXT)
    public String test_child_field1;

}
