package com.escli4j.dao;

import com.escli4j.model.EsEntity;
import com.escli4j.util.JsonUtils;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.action.index.IndexResponse;

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
        return create(obj, false);
    }

    /**
     * Creates document
     * @param obj document to create
     * @param refresh refresh index configuration
     * @return same object with id
     */
    public T create(T obj, boolean refresh) {
        IndexRequestBuilder req = prepareIndex(obj.getId()).setRefresh(refresh)
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
        return create(objs, false);
    }

    /**
     * Creates documents
     * @param objs documents to create
     * @param refresh refresh index configuration
     * @return same objects with ids
     */
    public List<T> create(List<T> objs, boolean refresh) {
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefresh(refresh);
            for (T obj : objs) {
                IndexRequestBuilder req = prepareIndex(obj.getId()).setSource(JsonUtils.writeValueAsBytes(obj));
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
     * @return successful shards number
     */
    public int update(T obj) {
        return update(obj, false, true);
    }

    /**
     * Update document
     * @param obj object to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return successful shards number
     */
    public int update(T obj, boolean refresh, boolean docAsUpsert) {
        return prepareUpdate(obj.getId()).setRefresh(refresh).setDocAsUpsert(docAsUpsert)
                .setDoc(JsonUtils.writeValueAsBytes(obj)).get().getShardInfo().getSuccessful();
    }

    /**
     * Update documents
     * @param objs objects to update
     * @return <strong>new</strong> array of objects that was updated. Consider object updated when the result of the
     * successful shards > 0
     */
    public List<T> update(List<T> objs) {
        return update(objs, false, true);
    }

    /**
     * Update documents
     * @param objs objects to update
     * @param refresh refresh index configuration
     * @param docAsUpsert should this doc be upserted or not
     * @return <strong>new</strong> array of objects that was updated/created. Consider object updated when the result
     * of the successful shards > 0
     */
    public List<T> update(List<T> objs, boolean refresh, boolean docAsUpsert) {
        ArrayList<T> retval = new ArrayList<>();
        if (objs.size() > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefresh(refresh);
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
     * @return result of the delete request
     */
    public boolean delete(String id) {
        return delete(id, false);
    }

    /**
     * Delete document
     * @param id document id to delete
     * @param refresh refresh index configuration
     * @return result of the delete request
     */
    public boolean delete(String id, boolean refresh) {
        return prepareDelete(id).setRefresh(refresh).get().isFound();
    }

    /**
     * Delete documents
     * @param ids document ids to delete
     * @return true if all documents deleted
     */
    public boolean delete(String... ids) {
        return delete(false, ids);
    }

    /**
     * Delete documents
     * @param refresh refresh index configuration
     * @param ids document ids to delete
     * @return true if all documents deleted
     */
    public boolean delete(boolean refresh, String... ids) {
        boolean retval = true;
        if (ids.length > 0) {
            BulkRequestBuilder bulk = prepareBulk().setRefresh(refresh);
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
