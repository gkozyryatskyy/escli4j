package com.escli4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EsEntity {

    @JsonIgnore
    protected String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "EsEntity [id=" + id + "]";
    }
}
