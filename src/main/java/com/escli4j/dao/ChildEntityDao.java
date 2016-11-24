package com.escli4j.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;

import com.escli4j.model.EsChildEntity;
import com.escli4j.util.JsonUtils;

public class ChildEntityDao<T extends EsChildEntity> extends Dao {

    protected final Class<T> clazz;

    public ChildEntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
        this.clazz = clazz;
    }

    public boolean isExist(String id, String parentId) {
        return prepareGet(id).setParent(parentId).get().isExists();
    }

    public T create(T obj) {
        return create(obj, RefreshPolicy.NONE);
    }

    public T create(T obj, RefreshPolicy refresh) {
        IndexResponse resp = prepareIndex(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh)
                .setOpType(OpType.CREATE).setSource(JsonUtils.writeValueAsBytes(obj)).get();
        obj.setId(resp.getId());
        return obj;
    }

    public void asyncCreate(T obj, Consumer<T> function) {
        asyncCreate(obj, RefreshPolicy.NONE, function);
    }

    public void asyncCreate(T obj, RefreshPolicy refresh, Consumer<T> function) {
        prepareIndex(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh).setOpType(OpType.CREATE)
                .setSource(JsonUtils.writeValueAsBytes(obj)).execute(new AbstractActionListener<IndexResponse>() {

                    @Override
                    public void onResponse(IndexResponse response) {
                        obj.setId(response.getId());
                        function.accept(obj);
                    }

                });
    }

    public List<T> create(List<T> objs) {
        return create(objs, RefreshPolicy.NONE);
    }

    public List<T> create(List<T> objs, RefreshPolicy refresh) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareIndex(obj.getId()).setParent(obj.getParent()).setOpType(OpType.CREATE)
                        .setSource(JsonUtils.writeValueAsBytes(obj)));
            }
            BulkResponse resp = bulk.get();
            for (BulkItemResponse item : resp.getItems()) {
                objs.get(item.getItemId()).setId(item.getId());
            }
        }
        return objs;
    }

    public T get(String id, String parentId) {
        GetResponse resp = prepareGet(id).setParent(parentId).get();
        if (resp.isExists()) {
            T obj = JsonUtils.read(resp.getSourceAsBytes(), clazz);
            obj.setId(resp.getId());
            obj.setParent(parentId);
            return obj;
        } else {
            return null;
        }
    }

    public void asyncGet(String id, String parentId, Consumer<T> function) {
        prepareGet(id).setParent(parentId).execute(new AbstractActionListener<GetResponse>() {

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

        });
    }

    /**
     * @param obj object to update
     * @return the total number of shards the write succeeded on (replicas and primaries). This includes relocating
     * shards, so this number can be higher than the number of shards.
     */
    public int update(T obj) {
        return update(obj, RefreshPolicy.NONE, true);
    }

    /**
     * @param obj object to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return the total number of shards the write succeeded on (replicas and primaries). This includes relocating
     * shards, so this number can be higher than the number of shards.
     */
    public int update(T obj, RefreshPolicy refresh, boolean docAsUpsert) {
        return prepareUpdate(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh)
                .setDocAsUpsert(docAsUpsert).setDoc(JsonUtils.writeValueAsBytes(obj)).get().getShardInfo()
                .getSuccessful();
    }

    public void asyncUpdate(T obj, Consumer<Integer> function) {
        asyncUpdate(obj, RefreshPolicy.NONE, true, function);
    }

    public void asyncUpdate(T obj, RefreshPolicy refresh, boolean docAsUpsert, Consumer<Integer> function) {
        prepareUpdate(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).execute(new AbstractActionListener<UpdateResponse>() {

                    @Override
                    public void onResponse(UpdateResponse response) {
                        function.accept(response.getShardInfo().getSuccessful());
                    }

                });
    }

    /**
     * @param objs objects to update
     * @return new array of objects that was updated. Consider object updated when the total number of shards the write
     * succeeded on more than 0.
     */
    public List<T> update(List<T> objs) {
        return update(objs, RefreshPolicy.NONE, true);
    }

    /**
     * @param objs objects to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return new array of objects that was updated. Consider object updated when the total number of shards the write
     * succeeded on more than 0.
     */
    public List<T> update(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert) {
        ArrayList<T> retval = new ArrayList<>();
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setParent(obj.getParent()).setDocAsUpsert(docAsUpsert)
                        .setDoc(JsonUtils.writeValueAsBytes(obj)));
            }
            BulkResponse resp = bulk.get();
            for (BulkItemResponse item : resp.getItems()) {
                if (item.getResponse().getShardInfo().getSuccessful() > 0) {
                    retval.add(objs.get(item.getItemId()));
                }
            }
        }
        return retval;
    }

    /**
     * Pass to function new array of objects that was updated. Consider object updated when the total number of shards
     * the write succeeded on more than 0.
     * @param objs objects to update
     * @param function operation which will be called after update execution
     */
    public void asyncUpdate(List<T> objs, Consumer<List<T>> function) {
        asyncUpdate(objs, RefreshPolicy.NONE, true, function);
    }

    /**
     * Pass to function new array of objects that was updated. Consider object updated when the total number of shards
     * the write succeeded on more than 0.
     * @param objs objects to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param function operation which will be called after update execution
     */
    public void asyncUpdate(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert, Consumer<List<T>> function) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setParent(obj.getParent()).setDocAsUpsert(docAsUpsert)
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

    public boolean delete(String id, String parentId) {
        return delete(id, parentId, RefreshPolicy.NONE);
    }

    public boolean delete(String id, String parentId, RefreshPolicy refresh) {
        return prepareDelete(id).setParent(parentId).setRefreshPolicy(refresh).get().getResult() == Result.DELETED;
    }
}
