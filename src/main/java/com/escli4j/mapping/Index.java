package com.escli4j.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.escli4j.model.EsEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Index {

    private String name;
    private Map<String, Class<? extends EsEntity>> types;
    private List<Annotation> annotations;

    public Index(String name) {
        this.name = name;
        types = new HashMap<>();
        annotations = new ArrayList<>();
    }

}
