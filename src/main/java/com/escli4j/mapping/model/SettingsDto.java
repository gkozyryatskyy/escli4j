package com.escli4j.mapping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class SettingsDto {

    private AnalysisDto analysis;

    public SettingsDto(AnalysisDto analysis) {
        if (analysis.getAnalyzer() != null || analysis.getFilter() != null) {
            setAnalysis(analysis);
        }
    }

}
