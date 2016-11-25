package com.escli4j.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
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
     */
    public void isExist(String id, Consumer<Boolean> function) {
        prepareGet(id).execute(new AbstractActionListener<GetResponse>() {

            @Override
            public void onResponse(GetResponse response) {
                function.accept(response.isExists());
            }
        });
    }

    /**
     * Asynchronous check document existence by id
     * @param function callback gets Set of the unique existed ids
     * @param ids document id
     */
    public void isExist(Consumer<Set<String>> function, String... ids) {
        if (ids.length > 0) {
            MultiGetRequestBuilder bulk = prepareMultiGet(ids);
            bulk.execute(new AbstractActionListener<MultiGetResponse>() {

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

            });
        } else {
            throw new IllegalArgumentException("Ids length must be > 0.");
        }
    }

    /**
     * Asynchronous creates document
     * @param obj document to create
     * @param function callback gets created document
     */
    public void create(T obj, Consumer<T> function) {
        create(obj, RefreshPolicy.NONE, function);
    }

    /**
     * Asynchronous creates document
     * @param obj document to create
     * @param refresh refresh index configuration
     * @param function callback gets created document
     */
    public void create(T obj, RefreshPolicy refresh, Consumer<T> function) {
        prepareIndex(obj.getId()).setRefreshPolicy(refresh).setOpType(OpType.CREATE)
                .setSource(JsonUtils.writeValueAsBytes(obj)).execute(new AbstractActionListener<IndexResponse>() {

                    @Override
                    public void onResponse(IndexResponse response) {
                        obj.setId(response.getId());
                        function.accept(obj);
                    }

                });
    }

    /**
     * Asynchronous creates documents
     * @param objs documents to create
     * @param function callback gets same objects with ids
     */
    public void create(List<T> obj, Consumer<List<T>> function) {
        create(obj, RefreshPolicy.NONE, function);
    }

    /**
     * Asynchronous creates documents
     * @param objs documents to create
     * @param refresh refresh index configuration
     * @param function callback gets same objects with ids
     */
    public void create(List<T> objs, RefreshPolicy refresh, Consumer<List<T>> function) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(
                        prepareIndex(obj.getId()).setOpType(OpType.CREATE).setSource(JsonUtils.writeValueAsBytes(obj)));
            }

            bulk.execute(new AbstractActionListener<BulkResponse>() {

                @Override
                public void onResponse(BulkResponse response) {
                    for (BulkItemResponse item : response.getItems()) {
                        objs.get(item.getItemId()).setId(item.getId());
                    }
                    function.accept(objs);
                }

            });
        }
    }

    /**
     * Asynchronous get document
     * @param id document id
     * @param function callback gets document with id
     */
    public void get(String id, Consumer<T> function) {
        prepareGet(id).execute(new AbstractActionListener<GetResponse>() {

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

        });
    }

    /**
     * Asynchronous gets documents
     * @param ids documents ids
     * @param function callback gets documents with ids
     */
    public void get(Consumer<Map<String, T>> function, String... ids) {
        if (ids.length > 0) {
            MultiGetRequestBuilder bulk = prepareMultiGet(ids);
            bulk.execute(new AbstractActionListener<MultiGetResponse>() {

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

            });
        } else {
            throw new IllegalArgumentException("Ids length must be > 0.");
        }
    }

    /**
     * Asynchronous update document
     * @param obj object to update
     * @param function callback gets the total number of shards the write succeeded on (replicas and primaries). This
     * includes relocating shards, so this number can be higher than the number of shards.
     */
    public void update(T obj, Consumer<Integer> function) {
        update(obj, RefreshPolicy.NONE, true, function);
    }

    /**
     * Asynchronous update document
     * @param obj object to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param function callback gets the total number of shards the write succeeded on (replicas and primaries). This
     * includes relocating shards, so this number can be higher than the number of shards.
     */
    public void update(T obj, RefreshPolicy refresh, boolean docAsUpsert, Consumer<Integer> function) {
        prepareUpdate(obj.getId()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).execute(new AbstractActionListener<UpdateResponse>() {

                    @Override
                    public void onResponse(UpdateResponse response) {
                        function.accept(response.getShardInfo().getSuccessful());
                    }

                });
    }

    /**
     * Asynchronous update documents 
     * Pass to function new array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     * @param objs objects to update
     * @param function callback gets <strong>new</strong> array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     */
    public void update(List<T> objs, Consumer<List<T>> function) {
        update(objs, RefreshPolicy.NONE, true, function);
    }

    /**
     * Asynchronous update documents 
     * Pass to function new array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     * @param objs objects to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param function callback gets <strong>new</strong> array of objects that was updated. Consider object updated
     * when the total number of shards the write succeeded on more than 0.
     */
    public void update(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert, Consumer<List<T>> function) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setDocAsUpsert(docAsUpsert)
                        .setDoc(JsonUtils.writeValueAsBytes(obj)));
            }
            bulk.execute(new AbstractActionListener<BulkResponse>() {

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

            });
        }
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param function callback gets true if document deleted
     */
    public void delete(String id, Consumer<Boolean> function) {
        delete(id, RefreshPolicy.NONE, function);
    }

    /**
     * Asynchronous delete document
     * @param id document id to delete
     * @param refresh refresh index configuration
     * @param function callback gets true if document deleted
     */
    public void delete(String id, RefreshPolicy refresh, Consumer<Boolean> function) {
        prepareDelete(id).setRefreshPolicy(refresh).execute(new AbstractActionListener<DeleteResponse>() {

            @Override
            public void onResponse(DeleteResponse response) {
                function.accept(response.getResult() == Result.DELETED);
            }
        });
    }

    /**
     * Asynchronous delete documents
     * @param function callback gets true if document deleted
     * @param ids document ids to delete
     */
    public void delete(Consumer<Boolean> function, String... ids) {
        delete(RefreshPolicy.NONE, function, ids);
    }

    /**
     * Asynchronous delete documents
     * @param function callback gets true if document deleted
     * @param refresh refresh index configuration
     * @param ids document ids to delete
     */
    public void delete(RefreshPolicy refresh, Consumer<Boolean> function, String... ids) {

        if (ids.length > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (String id : ids) {
                bulk.add(prepareDelete(id));
            }
            bulk.execute(new AbstractActionListener<BulkResponse>() {

                @Override
                public void onResponse(BulkResponse response) {
                    boolean retval = true;
                    for (BulkItemResponse item : response.getItems()) {
                        retval &= !item.isFailed();
                    }
                    function.accept(retval);
                }
            });

        }
    }

}
