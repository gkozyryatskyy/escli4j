package com.escli4j.dao;

import java.util.function.Consumer;

import org.elasticsearch.action.ActionListener;

public class ActionHandler<T> implements ActionListener<T> {

    private Consumer<T> responseFunction;
    private Consumer<Exception> failFunction;

    public ActionHandler(Consumer<T> responseFunction, Consumer<Exception> failFunction) {
        this.responseFunction = responseFunction;
        this.failFunction = failFunction;
    }

    @Override
    public void onResponse(T response) {
        responseFunction.accept(response);
    }

    @Override
    public void onFailure(Exception e) {
        failFunction.accept(e);
    }

}
