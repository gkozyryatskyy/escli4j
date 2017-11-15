package com.escli4j.mapping.model;

import com.escli4j.annotations.Context;
import com.escli4j.annotations.Context.ContextType;
import com.escli4j.annotations.Contexts;
import com.escli4j.annotations.CustomAnalyzer;
import com.escli4j.annotations.EdgeNGramFilter;
import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;
import com.escli4j.model.EsEntity;

@Type(index = "test1", type = "test1", update = true)
@EdgeNGramFilter(name = "test_edge_ngram_filter")
@CustomAnalyzer(name = "test_custom_analyzer", tokenizer = "standard",
        filter = { "lowercase", "test_edge_ngram_filter" })
@CustomAnalyzer(name = "lowercase", tokenizer = "keyword", filter = { "lowercase" })
public class TestMappingModel1 extends EsEntity {

    @Field(dataType = DataType.TEXT, analyzer = "standard", search_analyzer = "standard")
    public String field1;
    @Field(dataType = DataType.KEYWORD)
    public String field2;
    @Field(dataType = DataType.OBJECT)
    public Inner field3;
    @Field(dataType = DataType.KEYWORD, fields = { @InnerField(name = "indexes", dataType = DataType.TEXT,
            analyzer = "standard", search_analyzer = "standard") })
    public String field4;

    public static class Inner {
        @Field(dataType = DataType.TEXT)
        public Inner field5;
        @Field(dataType = DataType.KEYWORD)
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

    @Field(dataType = DataType.KEYWORD, fields = { @InnerField(name = "suggest", dataType = DataType.COMPLETION) })
    @Contexts({ @Context(name = "catName", type = ContextType.CATEGORY, path = "cat"),
            @Context(name = "geoName", type = ContextType.GEO, path = "geo") })
    public String field7;

}
