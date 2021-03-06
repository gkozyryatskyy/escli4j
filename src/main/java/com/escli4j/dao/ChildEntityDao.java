package com.escli4j.dao;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.get.GetResult;

import com.escli4j.model.EsChildEntity;
import com.escli4j.util.EscliJsonUtils;

public class ChildEntityDao<T extends EsChildEntity> extends Dao {

    private static final String _parent = "_parent";

    protected final Class<T> clazz;

    public ChildEntityDao(Class<T> clazz, Client client) {
        super(clazz, client);
        this.clazz = clazz;
    }

    protected T newObject(GetResponse response) {
        T retval = EscliJsonUtils.read(response.getSourceAsBytes(), clazz);
        retval.setId(response.getId());
        retval.setParent(response.getField(_parent).getValue().toString());
        return retval;
    }

    protected T newObject(GetResult result, String parentId) {
        T retval = EscliJsonUtils.read(result.source(), clazz);
        retval.setId(result.getId());
        // There is no parentId (_parent) field in update api response
        // https://discuss.elastic.co/t/update-api-missing-parent-in-response/122555
        retval.setParent(parentId);
        return retval;
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
                .setSource(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON);
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
                        .setSource(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON);
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
            return newObject(resp);
        } else {
            return null;
        }
    }

    /**
     * Update document
     * @param obj object to update
     * @return updated document
     */
    public T update(T obj) {
        return update(obj, RefreshPolicy.NONE, true, false);
    }

    /**
     * Update document
     * @param obj object to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param nullWithNoop return null if there was a noop
     * @return updated document
     */
    public T update(T obj, RefreshPolicy refresh, boolean docAsUpsert, boolean nullWithNoop) {
        UpdateResponse response = prepareUpdate(obj.getId()).setParent(obj.getParent()).setRefreshPolicy(refresh)
                .setDocAsUpsert(docAsUpsert).setFetchSource(true)
                .setDoc(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON).get();
        if (nullWithNoop && Result.NOOP == response.getResult()) {
            return null;
        } else {
            return newObject(response.getGetResult(), obj.getParent());
        }
    }

    /**
     * @param objs objects to update
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the result of the
     * update request is UPDATED
     */
    public List<T> update(List<T> objs) {
        return update(objs, RefreshPolicy.NONE, true, false);
    }

    /**
     * @param objs objects to update
     * @param refresh refresh configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param nullWithNoop return null if there was a noop
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the result of the
     * update request is UPDATED
     */
    public List<T> update(List<T> objs, RefreshPolicy refresh, boolean docAsUpsert, boolean nullWithNoop) {
        ArrayList<T> retval = new ArrayList<>();
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefreshPolicy(refresh);
            for (T obj : objs) {
                bulk.add(prepareUpdate(obj.getId()).setParent(obj.getParent()).setDocAsUpsert(docAsUpsert)
                        .setFetchSource(true).setDoc(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON));
            }
            BulkResponse resp = bulk.get();
            for (BulkItemResponse item : resp.getItems()) {
                T obj = objs.get(item.getItemId());
                UpdateResponse response = item.getResponse();
                if (nullWithNoop && Result.NOOP == response.getResult()) {
                    // do nothing
                } else {
                    retval.add(newObject(response.getGetResult(), obj.getParent()));
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
