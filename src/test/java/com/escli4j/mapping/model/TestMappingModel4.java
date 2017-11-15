package com.escli4j.mapping.model;

import com.escli4j.annotations.CustomNormalizer;
import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;
import com.escli4j.model.EsEntity;

@CustomNormalizer(name = "test_custom_normalizer", filter = { "lowercase" })
@Type(index = "test4", type = "test4")
public class TestMappingModel4 extends EsEntity {

    @Field(dataType = DataType.TEXT,
            fields = { @InnerField(name = "keyword", dataType = DataType.KEYWORD, normalizer = "test_custom_normalizer") })
    public String field1;
    @Field(dataType = DataType.KEYWORD, normalizer = "test_custom_normalizer")
    public String field2;

}
