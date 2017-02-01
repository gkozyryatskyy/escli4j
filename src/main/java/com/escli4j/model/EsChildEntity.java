package com.escli4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EsChildEntity extends EsEntity {

    private static final long serialVersionUID = -7759677956587776767L;

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
