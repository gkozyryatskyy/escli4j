package com.escli4j.mapping.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class AnalysisDto {

    private Map<String, TypeDto> filter;
    private Map<String, TypeDto> analyzer;
    private Map<String, TypeDto> normalizer;

    public AnalysisDto(Map<String, TypeDto> filter, Map<String, TypeDto> analyzer, Map<String, TypeDto> normalizer) {
        if (!filter.isEmpty()) {
            setFilter(filter);
        }
        if (!analyzer.isEmpty()) {
            setAnalyzer(analyzer);
        }
        if (!normalizer.isEmpty()) {
            setNormalizer(normalizer);
        }
    }

}
