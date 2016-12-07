package com.escli4j.mapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.escli4j.annotations.Type;
import com.escli4j.model.EsChildEntity;
import com.escli4j.model.EsEntity;

public class Mapping {

    private static final Logger log = LoggerFactory.getLogger(Mapping.class);
    // <index name, <type name, model class>>
    protected final Map<String, Map<String, Class<? extends EsEntity>>> model = new HashMap<>();
    protected final Client client;

    public Mapping(Client client) {
        this(null, client);
    }

    public Mapping(String modelPackage, Client client) {
        // get annotated classes
        Set<Class<?>> classes = MappingReflectUtils.getAnnotatedClasses(modelPackage, Type.class);
        // fill model map
        for (Class<?> clazz : classes) {
            Type typeAmmotation = clazz.getAnnotation(Type.class);
            // check inheritance
            if ("".equals(typeAmmotation.parent())) {
                if (!EsEntity.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException(clazz + " not an instance of " + EsEntity.class);
                }
            } else {
                if (!EsChildEntity.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException(clazz + " not an instance of " + EsChildEntity.class);
                }
            }
            // fill indexes map
            Map<String, Class<? extends EsEntity>> typesMap = model.get(typeAmmotation.index());
            if (typesMap == null) {
                typesMap = new HashMap<>();
                model.put(typeAmmotation.index(), typesMap);
            }
            // fill types max
            @SuppressWarnings("unchecked")
            Class<? extends EsEntity> prev = typesMap.put(typeAmmotation.type(), (Class<? extends EsEntity>) clazz);
            if (prev != null) {
                throw new IllegalStateException("THere is duplicate model classes " + prev + " and " + clazz);
            }
        }
        // save client
        this.client = client;
    }

    public void migrate() throws IOException {
        model.forEach((k, v) -> migrateIndex(k, v));
    }

    private void migrateIndex(String index, Map<String, Class<? extends EsEntity>> typesMap) {
        if (!isIndexExists(index)) {
            log.info("{} index not exists, creating...", index);
            createIndex(index, typesMap);
            log.info("{} index created.", index);
        } else {
            log.info("{} index exists, updating...", index);
            updateIndex(index, typesMap);
            log.info("{} index updated.", index);
        }
    }

    protected boolean isIndexExists(String index) {
        return client.admin().indices().prepareExists(index).get().isExists();
    }

    protected boolean createIndex(String index, Map<String, Class<? extends EsEntity>> typesMap) {
        CreateIndexRequestBuilder builder = client.admin().indices().prepareCreate(index);
        boolean execute = false;
        for (Map.Entry<String, Class<? extends EsEntity>> entry : typesMap.entrySet()) {
            Type typeAmmotation = entry.getValue().getAnnotation(Type.class);
            if (typeAmmotation.create()) {
                execute = true;
                builder.addMapping(entry.getKey(),
                        MappingUtils.getMappingBuilder(entry.getKey(), typeAmmotation.parent(), entry.getValue()));
            }
        }
        // not send get request if execute == false
        return execute && builder.get().isAcknowledged();
    }

    protected boolean updateIndex(String index, Map<String, Class<? extends EsEntity>> typesMap) {
        boolean execute = false;
        boolean result = true;
        for (Map.Entry<String, Class<? extends EsEntity>> entry : typesMap.entrySet()) {
            Type typeAmmotation = entry.getValue().getAnnotation(Type.class);
            if (typeAmmotation.update()) {
                execute = true;
                result &= client.admin().indices()
                        .preparePutMapping(index).setType(entry.getKey()).setSource(MappingUtils
                                .getMappingBuilder(entry.getKey(), typeAmmotation.parent(), entry.getValue()))
                        .get().isAcknowledged();
            }
        }
        return execute && result;
    }

}
