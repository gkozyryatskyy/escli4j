package com.escli4j.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;

import com.escli4j.model.EsEntity;
import com.escli4j.util.JsonUtils;

public class AsyncEntityDao<T extends EsEntity> extends EntityDao<T> {

    public AsyncEntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
    }

    /**
     * Asynchronous check document existence by id
     * @param id document id
     * @param function callback gets true if object exists
     * @param errorFunction callback gets exception on failure
     */
    public void isExist(String id, Consumer<Boolean> function, Consumer<Throwable> errorFunction) {
        prepareGet(id).execute(new ActionListener<GetResponse>() {

            @Override
            public void onResponse(GetResponse response) {
                function.accept(response.isExists());
            }

            @Override
            public void onFailure(Throwable e) {
                errorFunction.accept(e);
            }

        });
    }

    /**
     * Asynchronous check document existence by id
     * @param function callback gets Set of the unique existed ids
     * @param errorFunction callback gets exception on failure
     * @param ids document id
     */
    public void isExist(Consumer<Set<String>> function, Consumer<Throwable> errorFunction, String... ids) {
        if (ids.length > 0) {
            MultiGetRequestBuilder bulk = prepareMultiGet(ids);
            bulk.execute(new ActionListener<MultiGetResponse>() {

                @Override
                public void onResponse(MultiGetResponse response) {
                    Set<String> retval = new HashSet<>(response.getResponses().length);
                    for (MultiGetItemResponse item : response.getResponses()) {
                        GetResponse resp = item.getResponse();
                        if (resp.isExists()) {
                            retval.add(resp.getId());
                        }
                    }
                    function.accept(retval);
                }

                @Override
                public void onFailure(Throwable e) {
                    errorFunction.accept(e);
                }

            });
        } else {
            throw new IllegalArgumentException("Ids length must be > 0.");
        }
    }

    /**
     * Asynchronous creates document
     * @param obj document to create
     * @param function callback gets created document
     * @param errorFunction callback gets exception on failure
     */
    public void create(T obj, Consumer<T> function, Consumer<Throwable> errorFunction) {
        create(obj, false, function, errorFunction);
    }

    /**
     * Asynchronous creates document
     * @param obj document to create
     * @param refresh refresh index configuration
     * @param function callback gets created document
     * @param errorFunction callback gets exception on failure
     */
    public void create(T obj, boolean refresh, Consumer<T> function, Consumer<Throwable> errorFunction) {
        IndexRequestBuilder req = prepareIndex(obj.getId()).setRefresh(refresh)
                .setSource(JsonUtils.writeValueAsBytes(obj));
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
            public void onFailure(Throwable e) {
                errorFunction.accept(e);
            }

        });
    }

    /**
     * Asynchronous creates documents
     * @param obj documents to create
     * @param function callback gets same objects with ids
     * @param errorFunction callback gets exception on failure
     */
    public void create(List<T> obj, Consumer<List<T>> function, Consumer<Throwable> errorFunction) {
        create(obj, false, function, errorFunction);
    }

    /**
     * Asynchronous creates documents
     * @param objs documents to create
     * @param refresh refresh index configuration
     * @param function callback gets same objects with ids
     * @param errorFunction callback gets exception on failure
     */
    public void create(List<T> objs, boolean refresh, Consumer<List<T>> function, Consumer<Throwable> errorFunction) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefresh(refresh);
            for (T obj : objs) {
                IndexRequestBuilder req = prepareIndex(obj.getId()).setSource(JsonUtils.writeValueAsBytes(obj));
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
                public void onFailure(Throwable e) {
                    errorFunction.accept(e);
                }

            });
        }
    }

    /**
     * Asynchronous get document
     * @param id document id
     * @param function callback gets document with id
     * @param errorFunction callback gets exception on failure
     */
    public void get(String id, Consumer<T> function, Consumer<Throwable> errorFunction) {
        prepareGet(id).execute(new ActionListener<GetResponse>() {

            @Override
            public void onResponse(GetResponse response) {
                if (response.isExists()) {
                    T obj = JsonUtils.read(response.getSourceAsBytes(), clazz);
                    obj.setId(response.getId());
                    function.accept(obj);
                } else {
                    function.accept(null);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                errorFunction.accept(e);
            }

        });
    }

    /**
     * Asynchronous gets documents
     * @param function callback gets documents with ids
     * @param errorFunction callback gets exception on failure
     * @param ids documents ids
     */
    public void get(Consumer<Map<String, T>> function, Consumer<Throwable> errorFunction, String... ids) {
        if (ids.length > 0) {
            MultiGetRequestBuilder bulk = prepareMultiGet(ids);
            bulk.execute(new ActionListener<MultiGetResponse>() {

                @Override
                public void onResponse(MultiGetResponse response) {
                    Map<String, T> retval = new HashMap<>(response.getResponses().length);
                    for (MultiGetItemResponse item : response.getResponses()) {
                        GetResponse resp = item.getResponse();
                        if (resp.isExists()) {
                            T obj = JsonUtils.read(resp.getSourceAsBytes(), clazz);
                            obj.setId(resp.getId());
                            retval.put(resp.getId(), obj);
                        }
                    }
                    function.accept(retval);
                }

                @Override
                public void onFailure(Throwable e) {
                    errorFunction.accept(e);
                }

            });
        } else {
            throw new IllegalArgumentException("Ids length must be > 0.");
        }
    }

    /**
     * Asynchronous update document
     * @param obj object to update
     * @param function callback gets successful shards number
     * @param errorFunction callback gets exception on failure
     */
    public void update(T obj, IntConsumer function, Consumer<Throwable> errorFunction) {
        update(obj, false, true, function, errorFunction);
    }

    /**
     * Asynchronous update document
     * @param obj object to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param function callback gets successful shards number
     * @param errorFunction callback gets exception on failure
     */
    public void update(T obj, boolean refresh, boolean docAsUpsert, IntConsumer function,
            Consumer<Throwable> errorFunction) {
        prepareUpdate(obj.getId()).setRefresh(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).execute(new ActionListener<UpdateResponse>() {

                    @Override
                    public void onResponse(UpdateResponse response) {
                        function.accept(response.getShardInfo().getSuccessful());
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        errorFunction.accept(e);
                    }

                });
    }

    /**
     * Asynchronous update documents Pass to function new array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     * @param objs objects to update
     * @param function callback gets <strong>new</strong> array of objects that was updated. Consider object updated
     * when the result of the successful shards > 0
     * @param errorFunction callback gets exception on failure
     */
    public void update(List<T> objs, Consumer<List<T>> function, Consumer<Throwable> errorFunction) {
        update(objs, false, true, function, errorFunction);
    }

    /**
     * Asynchronous update documents Pass to function new array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     * @param objs objects to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param function callback gets <strong>new</strong> array of objects that was updated. Consider object updated
     * when the result of the successful shards > 0
     * @param errorFunction callback gets exception on failure
     */
    public void update(List<T> objs, boolean refresh, boolean docAsUpsert, Consumer<List<T>> function,
            Consumer<Throwable> errorFunction) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefresh(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setDocAsUpsert(docAsUpsert)
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
                public void onFailure(Throwable e) {
                    errorFunction.accept(e);
                }

            });
        }
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param function callback gets result of the delete request
     * @param errorFunction callback gets exception on failure
     */
    public void delete(String id, Consumer<Boolean> function, Consumer<Throwable> errorFunction) {
        delete(id, false, function, errorFunction);
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param refresh refresh index configuration
     * @param function callback gets result of the delete request
     * @param errorFunction callback gets exception on failure
     */
    public void delete(String id, boolean refresh, Consumer<Boolean> function, Consumer<Throwable> errorFunction) {
        prepareDelete(id).setRefresh(refresh).execute(new ActionListener<DeleteResponse>() {

            @Override
            public void onResponse(DeleteResponse response) {
                function.accept(response.isFound());
            }

            @Override
            public void onFailure(Throwable e) {
                errorFunction.accept(e);
            }
        });
    }

    /**
     * Asynchronous delete documents
     * @param function callback gets true if all documents deleted
     * @param errorFunction callback gets exception on failure
     * @param ids document ids to delete
     */
    public void delete(Consumer<Boolean> function, Consumer<Throwable> errorFunction, String... ids) {
        delete(false, function, errorFunction, ids);
    }

    /**
     * Asynchronous delete documents
     * @param refresh refresh index configuration
     * @param function callback gets true if all documents deleted
     * @param errorFunction callback gets exception on failure
     * @param ids document ids to delete
     */
    public void delete(boolean refresh, Consumer<Boolean> function, Consumer<Throwable> errorFunction,
            String... ids) {

        if (ids.length > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefresh(refresh);
            for (String id : ids) {
                bulk.add(prepareDelete(id));
            }
            bulk.execute(new ActionListener<BulkResponse>() {

                @Override
                public void onResponse(BulkResponse response) {
                    boolean retval = true;
                    for (BulkItemResponse item : response.getItems()) {
                        retval &= !item.isFailed();
                    }
                    function.accept(retval);
                }

                @Override
                public void onFailure(Throwable e) {
                    errorFunction.accept(e);
                }
            });

        }
    }

}
