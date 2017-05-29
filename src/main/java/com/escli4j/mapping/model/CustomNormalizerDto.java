package com.escli4j.mapping.model;
import com.escli4j.annotations.CustomNormalizer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class CustomNormalizerDto extends TypeDto {

    private String[] filter;

    public CustomNormalizerDto(CustomNormalizer annotation) {
        setType(CustomNormalizer.type);
        if (annotation.filter().length > 0) {
            setFilter(annotation.filter());
        }
    }

}
