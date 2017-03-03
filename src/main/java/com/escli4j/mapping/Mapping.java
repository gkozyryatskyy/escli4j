package com.escli4j.mapping;

import java.io.IOException;
import java.util.Arrays;
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
    protected final Map<String, Index> model = new HashMap<>();
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
            Index index = model.get(typeAmmotation.index());
            if (index == null) {
                index = new Index(typeAmmotation.index());
                model.put(typeAmmotation.index(), index);
            }
            // fill types map
            @SuppressWarnings("unchecked")
            Class<? extends EsEntity> prev = index.getTypes().put(typeAmmotation.type(),
                    (Class<? extends EsEntity>) clazz);
            if (prev != null) {
                throw new IllegalStateException("THere is duplicate model classes " + prev + " and " + clazz);
            }
            // fill settings list
            index.getAnnotations().addAll(Arrays.asList(clazz.getAnnotations()));
        }
        // save client
        this.client = client;
    }

    public void migrate() throws IOException {
        model.forEach((k, v) -> migrateIndex(v));
    }

    private void migrateIndex(Index indexObject) {
        if (!isIndexExists(indexObject.getName())) {
            log.info("{} index not exists, creating...", indexObject.getName());
            createIndex(indexObject);
            log.info("{} index created.", indexObject.getName());
        } else {
            log.info("{} index exists, updating...", indexObject.getName());
            updateIndex(indexObject);
            log.info("{} index updated.", indexObject.getName());
        }
    }

    protected boolean isIndexExists(String index) {
        return client.admin().indices().prepareExists(index).get().isExists();
    }

    protected boolean createIndex(Index index) {
        CreateIndexRequestBuilder builder = client.admin().indices().prepareCreate(index.getName());
        boolean execute = false;
        // build mappings
        for (Map.Entry<String, Class<? extends EsEntity>> entry : index.getTypes().entrySet()) {
            Type typeAmmotation = entry.getValue().getAnnotation(Type.class);
            if (typeAmmotation.create()) {
                execute = true;
                builder.addMapping(entry.getKey(),
                        MappingUtils.getMappingBuilder(entry.getKey(), typeAmmotation.parent(), entry.getValue()));
            }
        }
        // build settings
        builder.setSettings(MappingUtils.getSettingsBuilder(index.getAnnotations()));
        // not send get request if execute == false
        return execute && builder.get().isAcknowledged();
    }

    protected boolean updateIndex(Index index) {
        boolean execute = false;
        boolean result = true;
        for (Map.Entry<String, Class<? extends EsEntity>> entry : index.getTypes().entrySet()) {
            Type typeAmmotation = entry.getValue().getAnnotation(Type.class);
            if (typeAmmotation.update()) {
                execute = true;
                result &= client.admin().indices()
                        .preparePutMapping(index.getName()).setType(entry.getKey()).setSource(MappingUtils
                                .getMappingBuilder(entry.getKey(), typeAmmotation.parent(), entry.getValue()))
                        .get().isAcknowledged();
            }
        }
        return execute && result;
    }

}
