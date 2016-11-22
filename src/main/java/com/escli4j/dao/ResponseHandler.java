package com.escli4j.dao;

import java.util.function.Consumer;

public class ResponseHandler<T> extends AbstractActionListener<T> {

    private Consumer<T> function;

    public ResponseHandler(Consumer<T> function) {
        this.function = function;
    }

    @Override
    public void onResponse(T response) {
        function.accept(response);
    }

}
