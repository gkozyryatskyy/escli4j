package com.escli4j.dao;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Client;

import com.escli4j.model.EsChildEntity;
import com.escli4j.util.JsonUtils;

public class ChildEntityDao<T extends EsChildEntity> extends Dao {

    protected final Class<T> clazz;

    public ChildEntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
        this.clazz = clazz;
    }

    /**
     * Check document existence by id
     * @param id document id
     * @param parentId parent document id
     * @return true if object exists
     */
    public boolean isExist(String id, String parentId) {
        return prepareGet(id).setParent(parentId).get().isExists();
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
        IndexRequestBuilder req = prepareIndex(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh)
                .setSource(JsonUtils.writeValueAsBytes(obj));
        if (obj.getId() != null) {
            req.setOpType(OpType.CREATE);
        }
        IndexResponse resp = req.get();
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
                IndexRequestBuilder req = prepareIndex(obj.getId()).setParent(obj.getParent())
                        .setSource(JsonUtils.writeValueAsBytes(obj));
                if (obj.getId() != null) {
                    req.setOpType(OpType.CREATE);
                }
                bulk.add(req);
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
     * @param parentId parent document id
     * @return document with id
     */
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

    /**
     * Update document
     * @param obj object to update
     * @return result of the update request
     */
    public Result update(T obj) {
        return update(obj, RefreshPolicy.NONE, true);
    }

    /**
     * Update document
     * @param obj object to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return result of the update request
     */
    public Result update(T obj, RefreshPolicy refresh, boolean docAsUpsert) {
        return prepareUpdate(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh)
                .setDocAsUpsert(docAsUpsert).setDoc(JsonUtils.writeValueAsBytes(obj)).get().getResult();
    }

    /**
     * @param objs objects to update
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the result of the
     * update request is UPDATED
     */
    public List<T> update(List<T> objs) {
        return update(objs, RefreshPolicy.NONE, true);
    }

    /**
     * @param objs objects to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the result of the
     * update request is UPDATED
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
                if (item.getResponse().getResult() == Result.UPDATED) {
                    retval.add(objs.get(item.getItemId()));
                }
            }
        }
        return retval;
    }

    /**
     * Delete document
     * @param id document id to delete
     * @param parentId parent document id
     * @return result of the delete request
     */
    public Result delete(String id, String parentId) {
        return delete(id, parentId, RefreshPolicy.NONE);
    }

    /**
     * Delete document
     * @param id document id to delete
     * @param parentId parent document id
     * @param refresh refresh index configuration
     * @return result of the delete request
     */
    public Result delete(String id, String parentId, RefreshPolicy refresh) {
        return prepareDelete(id).setParent(parentId).setRefreshPolicy(refresh).get().getResult();
    }
}
