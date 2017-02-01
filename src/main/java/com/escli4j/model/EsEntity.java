package com.escli4j.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EsEntity implements Serializable {

    private static final long serialVersionUID = 3157546209065026044L;

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
