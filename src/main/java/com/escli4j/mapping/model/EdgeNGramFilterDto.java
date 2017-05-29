package com.escli4j.mapping.model;

import com.escli4j.annotations.EdgeNGramFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class EdgeNGramFilterDto extends TypeDto {

    private Integer min_gram;
    private Integer max_gram;

    public EdgeNGramFilterDto(EdgeNGramFilter annotation) {
        setType(EdgeNGramFilter.type);
        setMin_gram(annotation.min_gram());
        setMax_gram(annotation.max_gram());
    }

}
