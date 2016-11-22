package com.escli4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EsChildEntity extends EsEntity {

    @JsonIgnore
    protected String parent;

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "EsChildEntity [parent=" + parent + ", id=" + id + "]";
    }

}
