package com.escli4j.mapping.model;

import com.escli4j.annotations.Context;
import com.escli4j.annotations.Context.ContextType;
import com.escli4j.annotations.Contexts;
import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;
import com.escli4j.mapping.Index;

@Type(index = "test", type = "test")
public class TestMappingModel1 {

    @Field(dataType = DataType.STRING)
    public String field1;
    @Field(dataType = DataType.STRING, index = Index.NOT_ANALYZED)
    public String field2;
    @Field(dataType = DataType.OBJECT)
    public Inner field3;
    @Field(dataType = DataType.STRING, index = Index.NOT_ANALYZED,
            fields = { @InnerField(name = "indexes", dataType = DataType.STRING) })
    public String field4;

    public static class Inner {
        @Field(dataType = DataType.STRING)
        public Inner field5;
        @Field(dataType = DataType.STRING, index = Index.NOT_ANALYZED)
        public Inner field6;
    }

    @Field(dataType = DataType.COMPLETION)
    public String field5;

    @Field(dataType = DataType.COMPLETION)
    @Contexts({ @Context(name = "catName", type = ContextType.CATEGORY, path = "cat"),
            @Context(name = "geoName", type = ContextType.GEO, path = "geo") })
    public String field6;
    public String cat;
    public String geo;

    @Field(dataType = DataType.STRING, index = Index.NOT_ANALYZED,
            fields = { @InnerField(name = "suggest", dataType = DataType.COMPLETION) })
    @Contexts({ @Context(name = "catName", type = ContextType.CATEGORY, path = "cat"),
            @Context(name = "geoName", type = ContextType.GEO, path = "geo") })
    public String field7;

}
