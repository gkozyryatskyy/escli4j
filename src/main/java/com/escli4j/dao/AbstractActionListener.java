package com.escli4j.dao;

import org.elasticsearch.action.ActionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractActionListener<T> implements ActionListener<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractActionListener.class);

    @Override
    public abstract void onResponse(T response);

    @Override
    public void onFailure(Exception e) {
        log.error("Elasticsearch handler exception. ", e);
        throw new IllegalStateException(e);
    }

}
