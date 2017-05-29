package com.escli4j.mapping.model;

import com.escli4j.annotations.CustomAnalyzer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class CustomAnalyzerDto extends CustomNormalizerDto {

    private String tokenizer;

    public CustomAnalyzerDto(CustomAnalyzer annotation) {
        setType(CustomAnalyzer.type);
        setTokenizer(annotation.tokenizer());
        if (annotation.filter().length > 0) {
            setFilter(annotation.filter());
        }
    }

}
