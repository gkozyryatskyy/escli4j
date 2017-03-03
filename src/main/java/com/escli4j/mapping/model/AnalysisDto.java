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

    private Map<String, FilterDto> filter;
    private Map<String, AnalyzerDto> analyzer;

    public AnalysisDto(Map<String, FilterDto> filter, Map<String, AnalyzerDto> analyzer) {
        if (!filter.isEmpty()) {
            setFilter(filter);
        }
        if (!analyzer.isEmpty()) {
            setAnalyzer(analyzer);
        }
    }

}
