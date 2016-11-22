package com.escli4j.dao;

import com.escli4j.annotations.Type;

import java.util.function.Consumer;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Dao {

    public static final TimeValue scrollKeepAlive = new TimeValue(60000); // one minute

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final Client client;
    protected final String index;
    protected final String type;

    public Dao(Class<?> clazz, Client client) {
        if (clazz == null) {
            throw new IllegalStateException("Clazz == null. ");
        }
        Type typeAnnotation = clazz.getAnnotation(Type.class);
        if (typeAnnotation == null) {
            throw new IllegalStateException("Not annotated with " + Type.class);
        }
        index = typeAnnotation.index();
        type = typeAnnotation.type();
        if (client == null) {
            throw new IllegalStateException("Client == null. ");
        }
        this.client = client;
    }

    public BulkRequestBuilder prepareBulk() {
        return client.prepareBulk();
    }

    public SearchRequestBuilder prepareSearch() {
        return client.prepareSearch(index).setTypes(type);
    }

    public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
        return client.prepareSearchScroll(scrollId);
    }

    public IndexRequestBuilder prepareIndex() {
        return prepareIndex(null);
    }

    public IndexRequestBuilder prepareIndex(String id) {
        return client.prepareIndex(index, type, id);
    }

    public UpdateRequestBuilder prepareUpdate(String id) {
        return client.prepareUpdate(index, type, id);
    }

    public DeleteRequestBuilder prepareDelete(String id) {
        return client.prepareDelete(index, type, id);
    }

    public GetRequestBuilder prepareGet(String id) {
        return client.prepareGet(index, type, id);
    }

    public MultiGetRequestBuilder prepareMultiGet(String... ids) {
        return client.prepareMultiGet().add(index, type, ids);
    }

    public void refresh() {
        client.admin().indices().prepareRefresh(index).get();
    }

    public void scrollNext(String scrollId, Consumer<SearchResponse> function) {
        prepareSearchScroll(scrollId).setScroll(scrollKeepAlive).execute(new ResponseHandler<>(function));
    }
}
