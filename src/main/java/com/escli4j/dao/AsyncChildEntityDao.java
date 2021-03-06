package com.escli4j.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;

import com.escli4j.model.EsChildEntity;
import com.escli4j.util.EscliJsonUtils;

public class AsyncChildEntityDao<T extends EsChildEntity> extends ChildEntityDao<T> {

    public AsyncChildEntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
    }

    /**
     * Asynchronous check document existence by id
     * @param id document id
     * @param parentId parent document id
     * @param function callback gets true if object exists
     * @param errorFunction callback gets exception on failure
     */
    public void isExist(String id, String parentId, Consumer<Boolean> function, Consumer<Throwable> errorFunction) {
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
     * @param errorFunction callback gets exception on failure
     */
    public void create(T obj, Consumer<T> function, Consumer<Throwable> errorFunction) {
        create(obj, RefreshPolicy.NONE, function, errorFunction);
    }

    /**
     * Asynchronous creates document
     * @param obj document to create
     * @param refresh refresh index configuration
     * @param function callback created document
     * @param errorFunction callback gets exception on failure
     */
    public void create(T obj, RefreshPolicy refresh, Consumer<T> function, Consumer<Throwable> errorFunction) {
        IndexRequestBuilder req = prepareIndex(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh)
                .setSource(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON);
        if (obj.getId() != null) {
            req.setOpType(OpType.CREATE);
        }
        req.execute(new ActionListener<IndexResponse>() {

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
     * @param function callback same objects with ids
     * @param errorFunction callback gets exception on failure
     */
    public void create(List<T> obj, Consumer<List<T>> function, Consumer<Throwable> errorFunction) {
        create(obj, RefreshPolicy.NONE, function, errorFunction);
    }

    /**
     * Asynchronous creates documents
     * @param objs documents to create
     * @param refresh refresh index configuration
     * @param function callback same objects with ids
     * @param errorFunction callback gets exception on failure
     */
    public void create(List<T> objs, RefreshPolicy refresh, Consumer<List<T>> function,
            Consumer<Throwable> errorFunction) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                IndexRequestBuilder req = prepareIndex(obj.getId()).setParent(obj.getParent())
                        .setSource(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON);
                if (obj.getId() != null) {
                    req.setOpType(OpType.CREATE);
                }
                bulk.add(req);
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
        } else {
            function.accept(objs);
        }
    }

    /**
     * Asynchronous get document
     * @param id document id
     * @param parentId parent document id
     * @param function callback with document by id
     * @param errorFunction callback gets exception on failure
     */
    public void get(String id, String parentId, Consumer<T> function, Consumer<Throwable> errorFunction) {
        prepareGet(id).setParent(parentId).execute(new ActionListener<GetResponse>() {

            @Override
            public void onResponse(GetResponse response) {
                if (response.isExists()) {
                    function.accept(newObject(response));
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
     * @param function callback with updated document
     * @param errorFunction callback gets exception on failure
     */
    public void update(T obj, Consumer<T> function, Consumer<Throwable> errorFunction) {
        update(obj, RefreshPolicy.NONE, true, false, function, errorFunction);
    }

    /**
     * Asynchronous update document
     * @param obj object to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param nullWithNoop return null if there was a noop
     * @param function callback with updated document
     * @param errorFunction callback gets exception on failure
     */
    public void update(T obj, RefreshPolicy refresh, boolean docAsUpsert, boolean nullWithNoop, Consumer<T> function,
            Consumer<Throwable> errorFunction) {
        prepareUpdate(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setFetchSource(true).setDoc(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON)
                .execute(new ActionListener<UpdateResponse>() {

                    @Override
                    public void onResponse(UpdateResponse response) {
                        if (nullWithNoop && Result.NOOP == response.getResult()) {
                            function.accept(null);
                        } else {
                            function.accept(newObject(response.getGetResult(), obj.getParent()));
                        }
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
     * @param function callback gets <strong>new</strong> array of objects that was updated. Consider object updated
     * when the result of the update request is UPDATED
     * @param errorFunction callback gets exception on failure
     */
    public void update(List<T> objs, Consumer<List<T>> function, Consumer<Throwable> errorFunction) {
        update(objs, RefreshPolicy.NONE, true, false, function, errorFunction);
    }

    /**
     * Asynchronous update documents Pass to function new array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     * @param objs objects to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param nullWithNoop return null if there was a noop
     * @param function callback gets <strong>new</strong> array of objects that was updated. Consider object updated
     * when the result of the update request is UPDATED
     * @param errorFunction callback gets exception on failure
     */
    public void update(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert, boolean nullWithNoop,
            Consumer<List<T>> function, Consumer<Throwable> errorFunction) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setParent(obj.getParent()).setDocAsUpsert(docAsUpsert)
                        .setFetchSource(true).setDoc(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON));
            }
            bulk.execute(new ActionListener<BulkResponse>() {

                @Override
                public void onResponse(BulkResponse resp) {
                    ArrayList<T> retval = new ArrayList<>();
                    for (BulkItemResponse item : resp.getItems()) {
                        T obj = objs.get(item.getItemId());
                        UpdateResponse response = item.getResponse();
                        if (nullWithNoop && Result.NOOP == response.getResult()) {
                            // do nothing
                        } else {
                            retval.add(newObject(response.getGetResult(), obj.getParent()));
                        }
                    }
                    function.accept(retval);
                }

                @Override
                public void onFailure(Exception e) {
                    errorFunction.accept(e);
                }

            });
        } else {
            function.accept(objs);
        }
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param parentId parent document id
     * @param function callback gets result of the delete request
     * @param errorFunction callback gets exception on failure
     */
    public void delete(String id, String parentId, Consumer<Result> function, Consumer<Throwable> errorFunction) {
        delete(id, parentId, RefreshPolicy.NONE, function, errorFunction);
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param parentId parent document id
     * @param refresh refresh index configuration
     * @param function callback gets result of the delete request
     * @param errorFunction callback gets exception on failure
     */
    public void delete(String id, String parentId, RefreshPolicy refresh, Consumer<Result> function,
            Consumer<Throwable> errorFunction) {
        prepareDelete(id).setParent(parentId).setRefreshPolicy(refresh).execute(new ActionListener<DeleteResponse>() {

            @Override
            public void onResponse(DeleteResponse response) {
                function.accept(response.getResult());
            }

            @Override
            public void onFailure(Exception e) {
                errorFunction.accept(e);
            }
        });
    }

}
