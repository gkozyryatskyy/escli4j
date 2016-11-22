package com.escli4j.dao;

import com.escli4j.model.EsEntity;
import com.escli4j.util.JsonUtils;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class EntityDao<T extends EsEntity> extends Dao {

    protected final Class<T> clazz;

    public EntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
        this.clazz = clazz;
    }

    public boolean isExist(String id) {
        return prepareGet(id).get().isExists();
    }

    public void asyncExist(Consumer<Set<String>> function, String... ids) {
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

    public T create(T obj) {
        return create(obj, RefreshPolicy.NONE);
    }

    public T create(T obj, RefreshPolicy refresh) {
        IndexResponse resp = prepareIndex(obj.getId()).setRefreshPolicy(refresh).setOpType(OpType.CREATE)
                .setSource(JsonUtils.writeValueAsBytes(obj)).get();
        obj.setId(resp.getId());
        return obj;
    }

    public void asyncCreate(T obj, Consumer<T> function) {
        asyncCreate(obj, RefreshPolicy.NONE, function);
    }

    public void asyncCreate(T obj, RefreshPolicy refresh, Consumer<T> function) {
        prepareIndex(obj.getId()).setRefreshPolicy(refresh).setOpType(OpType.CREATE)
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
                bulk.add(
                        prepareIndex(obj.getId()).setOpType(OpType.CREATE).setSource(JsonUtils.writeValueAsBytes(obj)));
            }
            BulkResponse resp = bulk.get();
            for (BulkItemResponse item : resp.getItems()) {
                objs.get(item.getItemId()).setId(item.getId());
            }
        }
        return objs;
    }

    public T get(String id) {
        GetResponse resp = prepareGet(id).get();
        if (resp.isExists()) {
            T obj = JsonUtils.read(resp.getSourceAsBytes(), clazz);
            obj.setId(resp.getId());
            return obj;
        } else {
            return null;
        }
    }

    public void asyncGet(String id, Consumer<T> function) {
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

    public List<T> get(String... ids) {
        if (ids.length > 0) {
            MultiGetResponse response = prepareMultiGet(ids).get();
            List<T> retval = new ArrayList<>();
            for (MultiGetItemResponse item : response.getResponses()) {
                GetResponse resp = item.getResponse();
                if (resp.isExists()) {
                    T obj = JsonUtils.read(resp.getSourceAsBytes(), clazz);
                    obj.setId(resp.getId());
                    retval.add(obj);
                }
            }
            return retval;
        } else {
            throw new IllegalArgumentException("Ids length must be > 0.");
        }
    }

    public void asyncGet(Consumer<Map<String, T>> function, String... ids) {
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
     * @return the total number of shards the write succeeded on (replicas and primaries). This includes relocating
     * shards, so this number can be higher than the number of shards.
     */
    public int update(T obj) {
        return update(obj, RefreshPolicy.NONE, true);
    }

    /**
     * @return the total number of shards the write succeeded on (replicas and primaries). This includes relocating
     * shards, so this number can be higher than the number of shards.
     */
    public int update(T obj, RefreshPolicy refresh, boolean docAsUpsert) {
        return prepareUpdate(obj.getId()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).get().getShardInfo().getSuccessful();
    }

    public void asyncUpdate(T obj, Consumer<Integer> function) {
        asyncUpdate(obj, RefreshPolicy.NONE, true, function);
    }

    public void asyncUpdate(T obj, RefreshPolicy refresh, boolean docAsUpsert, Consumer<Integer> function) {
        prepareUpdate(obj.getId()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).execute(new AbstractActionListener<UpdateResponse>() {

                    @Override
                    public void onResponse(UpdateResponse response) {
                        function.accept(response.getShardInfo().getSuccessful());
                    }

                });
    }

    /**
     * @return new array of objects that was updated. Consider object updated when the total number of shards the write
     * succeeded on more than 0.
     */
    public List<T> update(List<T> objs) {
        return update(objs, RefreshPolicy.NONE, true);
    }

    /**
     * @return new array of objects that was updated. Consider object updated when the total number of shards the write
     * succeeded on more than 0.
     */
    public List<T> update(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert) {
        ArrayList<T> retval = new ArrayList<>();
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setDocAsUpsert(docAsUpsert)
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
     * @return new array of objects that was updated. Consider object updated when the total number of shards the write
     * succeeded on more than 0.
     */
    public void asyncUpdate(List<T> objs, Consumer<List<T>> function) {
        asyncUpdate(objs, RefreshPolicy.NONE, true, function);
    }

    /**
     * @return new array of objects that was updated. Consider object updated when the total number of shards the write
     * succeeded on more than 0.
     */
    public void asyncUpdate(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert, Consumer<List<T>> function) {
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

    public boolean delete(String id) {
        return delete(id, RefreshPolicy.NONE);
    }

    public boolean delete(String id, RefreshPolicy refresh) {
        return prepareDelete(id).setRefreshPolicy(refresh).get().getResult() == Result.DELETED;
    }

    public boolean delete(String... ids) {
        return delete(RefreshPolicy.NONE, ids);
    }

    public boolean delete(RefreshPolicy refresh, String... ids) {
        boolean retval = true;
        if (ids.length > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (String id : ids) {
                bulk.add(prepareDelete(id));
            }
            BulkResponse resp = bulk.get();
            for (BulkItemResponse item : resp.getItems()) {
                retval &= !item.isFailed();
            }
        }
        return retval;
    }
}
