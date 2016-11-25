package com.escli4j.dao;

import com.escli4j.model.EsEntity;
import com.escli4j.util.JsonUtils;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.client.Client;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityDao<T extends EsEntity> extends Dao {

    protected final Class<T> clazz;

    public EntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
        this.clazz = clazz;
    }

    /**
     * Check document existence by id
     * @param id document id
     * @return true if object exists
     */
    public boolean isExist(String id) {
        return prepareGet(id).get().isExists();
    }

    /**
     * Check documents array existence by ids
     * @param ids documents ids
     * @return Set of the unique existed ids
     */
    public Set<String> isExist(String... ids) {
        return Arrays.stream(prepareMultiGet(ids).get().getResponses()).filter(r -> r.getResponse().isExists())
                .map(r -> r.getResponse().getId()).collect(Collectors.toSet());
    }

    /**
     * Creates document
     * @param obj document to create
     * @return same object with id
     */
    public T create(T obj) {
        return create(obj, RefreshPolicy.NONE);
    }

    /**
     * Creates document
     * @param obj document to create
     * @param refresh refresh index configuration
     * @return same object with id
     */
    public T create(T obj, RefreshPolicy refresh) {
        IndexResponse resp = prepareIndex(obj.getId()).setRefreshPolicy(refresh).setOpType(OpType.CREATE)
                .setSource(JsonUtils.writeValueAsBytes(obj)).get();
        obj.setId(resp.getId());
        return obj;
    }

    /**
     * Creates documents
     * @param objs documents to create
     * @return same objects with ids
     */
    public List<T> create(List<T> objs) {
        return create(objs, RefreshPolicy.NONE);
    }

    /**
     * Creates documents
     * @param objs documents to create
     * @param refresh refresh index configuration
     * @return same objects with ids
     */
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

    /**
     * Get document
     * @param id document id
     * @return document with id
     */
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

    /**
     * Get documents
     * @param ids documents ids
     * @return documents with ids
     */
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

    /**
     * Update document
     * @param obj object to update
     * @return the total number of shards the write succeeded on (replicas and primaries). This includes relocating
     * shards, so this number can be higher than the number of shards.
     */
    public int update(T obj) {
        return update(obj, RefreshPolicy.NONE, true);
    }

    /**
     * Update document
     * @param obj object to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return the total number of shards the write succeeded on (replicas and primaries). This includes relocating
     * shards, so this number can be higher than the number of shards.
     */
    public int update(T obj, RefreshPolicy refresh, boolean docAsUpsert) {
        return prepareUpdate(obj.getId()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).get().getShardInfo().getSuccessful();
    }

    /**
     * Update documents
     * @param objs objects to update
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the total number of
     * shards the write succeeded on more than 0.
     */
    public List<T> update(List<T> objs) {
        return update(objs, RefreshPolicy.NONE, true);
    }

    /**
     * Update documents
     * @param objs objects to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the total number of
     * shards the write succeeded on more than 0.
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
     * Delete document
     * @param id document id to delete
     * @return true if document deleted
     */
    public boolean delete(String id) {
        return delete(id, RefreshPolicy.NONE);
    }

    /**
     * Delete document
     * @param id document id to delete
     * @param refresh refresh index configuration
     * @return true if document deleted
     */
    public boolean delete(String id, RefreshPolicy refresh) {
        return prepareDelete(id).setRefreshPolicy(refresh).get().getResult() == Result.DELETED;
    }

    /**
     * Delete documents
     * @param ids document ids to delete
     * @return true if document deleted
     */
    public boolean delete(String... ids) {
        return delete(RefreshPolicy.NONE, ids);
    }

    /**
     * Delete documents
     * @param refresh refresh index configuration
     * @param ids document ids to delete
     * @return true if document deleted
     */
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
