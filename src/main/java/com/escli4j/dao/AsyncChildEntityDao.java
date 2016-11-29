package com.escli4j.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;

import com.escli4j.model.EsChildEntity;
import com.escli4j.util.JsonUtils;

public class AsyncChildEntityDao<T extends EsChildEntity> extends ChildEntityDao<T> {

    public AsyncChildEntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
    }

    /**
     * Asynchronous check document existence by id
     * @param id document id
     * @param parentId parent document id
     * @param function callback gets true if object exists
     */
    public void isExist(String id, String parentId, Consumer<Boolean> function, Consumer<Exception> errorFunction) {
        prepareGet(id).setParent(parentId).execute(new ActionListener<GetResponse>() {

            @Override
            public void onResponse(GetResponse response) {
                function.accept(response.isExists());
            }

            @Override
            public void onFailure(Exception e) {
                errorFunction.accept(e);
            }
        });
    }

    /**
     * Asynchronous creates document
     * @param obj document to create
     * @param function callback gets created document
     */
    public void create(T obj, Consumer<T> function, Consumer<Exception> errorFunction) {
        create(obj, RefreshPolicy.NONE, function, errorFunction);
    }

    /**
     * Asynchronous creates document
     * @param obj document to create
     * @param refresh refresh index configuration
     * @param function callback gets created document
     */
    public void create(T obj, RefreshPolicy refresh, Consumer<T> function, Consumer<Exception> errorFunction) {
        prepareIndex(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh).setOpType(OpType.CREATE)
                .setSource(JsonUtils.writeValueAsBytes(obj)).execute(new ActionListener<IndexResponse>() {

                    @Override
                    public void onResponse(IndexResponse response) {
                        obj.setId(response.getId());
                        function.accept(obj);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        errorFunction.accept(e);
                    }

                });
    }

    /**
     * Asynchronous creates documents
     * @param obj documents to create
     * @param function callback gets same objects with ids
     */
    public void create(List<T> obj, Consumer<List<T>> function, Consumer<Exception> errorFunction) {
        create(obj, RefreshPolicy.NONE, function, errorFunction);
    }

    /**
     * Asynchronous creates documents
     * @param objs documents to create
     * @param refresh refresh index configuration
     * @param function callback gets same objects with ids
     */
    public void create(List<T> objs, RefreshPolicy refresh, Consumer<List<T>> function,
            Consumer<Exception> errorFunction) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareIndex(obj.getId()).setParent(obj.getParent()).setOpType(OpType.CREATE)
                        .setSource(JsonUtils.writeValueAsBytes(obj)));
            }

            bulk.execute(new ActionListener<BulkResponse>() {

                @Override
                public void onResponse(BulkResponse response) {
                    for (BulkItemResponse item : response.getItems()) {
                        objs.get(item.getItemId()).setId(item.getId());
                    }
                    function.accept(objs);
                }

                @Override
                public void onFailure(Exception e) {
                    errorFunction.accept(e);
                }

            });
        }
    }

    /**
     * Asynchronous get document
     * @param id document id
     * @param parentId parent document id
     * @param function callback gets document with id
     */
    public void get(String id, String parentId, Consumer<T> function, Consumer<Exception> errorFunction) {
        prepareGet(id).setParent(parentId).execute(new ActionListener<GetResponse>() {

            @Override
            public void onResponse(GetResponse response) {
                if (response.isExists()) {
                    T obj = JsonUtils.read(response.getSourceAsBytes(), clazz);
                    obj.setId(response.getId());
                    obj.setParent(parentId);
                    function.accept(obj);
                } else {
                    function.accept(null);
                }
            }

            @Override
            public void onFailure(Exception e) {
                errorFunction.accept(e);
            }

        });
    }

    /**
     * Asynchronous update document
     * @param obj object to update
     * @param function callback gets the total number of shards the write succeeded on (replicas and primaries). This
     * includes relocating shards, so this number can be higher than the number of shards.
     */
    public void update(T obj, Consumer<Integer> function, Consumer<Exception> errorFunction) {
        update(obj, RefreshPolicy.NONE, true, function, errorFunction);
    }

    /**
     * Asynchronous update document
     * @param obj object to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param function callback gets the total number of shards the write succeeded on (replicas and primaries). This
     * includes relocating shards, so this number can be higher than the number of shards.
     */
    public void update(T obj, RefreshPolicy refresh, boolean docAsUpsert, Consumer<Integer> function,
            Consumer<Exception> errorFunction) {
        prepareUpdate(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).execute(new ActionListener<UpdateResponse>() {

                    @Override
                    public void onResponse(UpdateResponse response) {
                        function.accept(response.getShardInfo().getSuccessful());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        errorFunction.accept(e);
                    }

                });
    }

    /**
     * Asynchronous update documents Pass to function new array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     * @param objs objects to update
     * @param function operation which will be called after update execution
     */
    public void update(List<T> objs, Consumer<List<T>> function, Consumer<Exception> errorFunction) {
        update(objs, RefreshPolicy.NONE, true, function, errorFunction);
    }

    /**
     * Asynchronous update documents Pass to function new array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     * @param objs objects to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param function operation which will be called after update execution
     */
    public void update(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert, Consumer<List<T>> function,
            Consumer<Exception> errorFunction) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setParent(obj.getParent()).setDocAsUpsert(docAsUpsert)
                        .setDoc(JsonUtils.writeValueAsBytes(obj)));
            }
            bulk.execute(new ActionListener<BulkResponse>() {

                @Override
                public void onResponse(BulkResponse response) {
                    ArrayList<T> retval = new ArrayList<>();
                    for (BulkItemResponse item : response.getItems()) {
                        if (item.getResponse().getShardInfo().getSuccessful() > 0) {
                            retval.add(objs.get(item.getItemId()));
                        }
                    }
                    function.accept(retval);
                }

                @Override
                public void onFailure(Exception e) {
                    errorFunction.accept(e);
                }

            });
        }
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param parentId parent document id
     * @param function callback gets true if document deleted
     */
    public void delete(String id, String parentId, Consumer<Boolean> function, Consumer<Exception> errorFunction) {
        delete(id, parentId, RefreshPolicy.NONE, function, errorFunction);
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param parentId parent document id
     * @param refresh refresh index configuration
     * @param function callback gets true if document deleted
     */
    public void delete(String id, String parentId, RefreshPolicy refresh, Consumer<Boolean> function,
            Consumer<Exception> errorFunction) {
        prepareDelete(id).setParent(parentId).setRefreshPolicy(refresh).execute(new ActionListener<DeleteResponse>() {

            @Override
            public void onResponse(DeleteResponse response) {
                function.accept(response.getResult() == Result.DELETED);
            }

            @Override
            public void onFailure(Exception e) {
                errorFunction.accept(e);
            }
        });
    }

}
