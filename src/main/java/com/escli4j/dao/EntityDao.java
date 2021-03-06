package com.escli4j.dao;

import com.escli4j.model.EsEntity;
import com.escli4j.util.EscliJsonUtils;

import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateResponse;

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

    protected T newObject(byte[] source, String id) {
        T retval = EscliJsonUtils.read(source, clazz);
        retval.setId(id);
        return retval;
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
        IndexRequestBuilder req = prepareIndex(obj.getId()).setRefreshPolicy(refresh)
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
                IndexRequestBuilder req = prepareIndex(obj.getId()).setSource(EscliJsonUtils.writeValueAsBytes(obj),
                        XContentType.JSON);
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
     * @return document with id
     */
    public T get(String id) {
        GetResponse resp = prepareGet(id).get();
        if (resp.isExists()) {
            return newObject(resp.getSourceAsBytes(), id);
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
                    retval.add(newObject(resp.getSourceAsBytes(), resp.getId()));
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
     * @return result of the update request
     */
    public T update(T obj) {
        return update(obj, RefreshPolicy.NONE, true, false);
    }

    /**
     * Update document
     * @param obj object to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @param nullWithNoop return null if there was a noop
     * @return result of the update request
     */
    public T update(T obj, RefreshPolicy refresh, boolean docAsUpsert, boolean nullWithNoop) {
        UpdateResponse response = prepareUpdate(obj.getId()).setRefreshPolicy(refresh).setDocAsUpsert(docAsUpsert)
                .setFetchSource(true).setDoc(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON).get();
        if (nullWithNoop) {
            if (response.getResult() != Result.NOOP) {
                return newObject(response.getGetResult().source(), obj.getId());
            } else {
                return null;
            }
        } else {
            return newObject(response.getGetResult().source(), obj.getId());
        }
    }

    /**
     * Update documents
     * @param objs objects to update
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the result of the
     * update request is UPDATED
     */
    public List<T> update(List<T> objs) {
        return update(objs, RefreshPolicy.NONE, true, false);
    }

    /**
     * Update documents
     * @param objs objects to update
     * @param refresh refresh index configuration
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
                bulk.add(prepareUpdate(obj.getId()).setDocAsUpsert(docAsUpsert).setFetchSource(true)
                        .setDoc(EscliJsonUtils.writeValueAsBytes(obj), XContentType.JSON));
            }
            BulkResponse resp = bulk.get();
            for (BulkItemResponse item : resp.getItems()) {
                UpdateResponse updateResponce = item.getResponse();
                T obj = objs.get(item.getItemId());
                if (nullWithNoop) {
                    if (updateResponce.getResult() != Result.NOOP) {
                        retval.add(newObject(updateResponce.getGetResult().source(), obj.getId()));
                    }
                } else {
                    retval.add(newObject(updateResponce.getGetResult().source(), obj.getId()));
                }
            }
        }
        return retval;
    }

    /**
     * Delete document
     * @param id document id to delete
     * @return result of the delete request
     */
    public Result delete(String id) {
        return delete(id, RefreshPolicy.NONE);
    }

    /**
     * Delete document
     * @param id document id to delete
     * @param refresh refresh index configuration
     * @return result of the delete request
     */
    public Result delete(String id, RefreshPolicy refresh) {
        return prepareDelete(id).setRefreshPolicy(refresh).get().getResult();
    }

    /**
     * Delete documents
     * @param ids document ids to delete
     * @return true if all documents deleted
     */
    public boolean delete(String... ids) {
        return delete(RefreshPolicy.NONE, ids);
    }

    /**
     * Delete documents
     * @param refresh refresh index configuration
     * @param ids document ids to delete
     * @return true if all documents deleted
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
                retval &= item.getResponse().getResult() == Result.DELETED;
            }
        }
        return retval;
    }
}
