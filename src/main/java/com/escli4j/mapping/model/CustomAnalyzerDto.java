package com.escli4j.mapping.model;

import com.escli4j.annotations.CustomAnalyzer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class CustomAnalyzerDto extends AnalyzerDto {

    private String tokenizer;
    private String[] filter;

    public CustomAnalyzerDto(CustomAnalyzer annotation) {
        setType(CustomAnalyzer.type);
        setTokenizer(annotation.tokenizer());
        if (annotation.filter().length > 0) {
            setFilter(annotation.filter());
        }
    }

}
