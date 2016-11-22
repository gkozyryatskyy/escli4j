package com.escli4j.mapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.escli4j.annotations.Type;
import com.escli4j.model.EsChildEntity;
import com.escli4j.model.EsEntity;
import com.escli4j.util.MappingUtils;
import com.escli4j.util.StaticProps;

@SuppressWarnings("unchecked")
public class Mapping {

    private static final Logger log = LoggerFactory.getLogger(Mapping.class);
    // <index name, <type name, model class>>
    protected static final Map<String, Map<String, Class<? extends EsEntity>>> model = new HashMap<>();

    static {
        Reflections reflections;
        String escliPackage = System.getProperty(StaticProps.PACKAGE);
        if (escliPackage != null) {
            reflections = new Reflections(escliPackage);
        } else {
            reflections = new Reflections();
        }

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Type.class);
        for (Class<?> clazz : classes) {
            // check inheritance
            if (!EsEntity.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException(
                        clazz + " not an instance of " + EsEntity.class + " or " + EsChildEntity.class);
            }
            Type typeAmmotation = clazz.getAnnotation(Type.class);
            // fill indexes map
            Map<String, Class<? extends EsEntity>> typesMap = model.get(typeAmmotation.index());
            if (typesMap == null) {
                typesMap = new HashMap<>();
                model.put(typeAmmotation.index(), typesMap);
            }
            // fill types max
            Class<? extends EsEntity> prev = typesMap.put(typeAmmotation.type(), (Class<? extends EsEntity>) clazz);
            if (prev != null) {
                throw new IllegalStateException("THere is duplicate model classes " + prev + " and " + clazz);
            }
        }
    }

    protected final Client client;

    public Mapping(Client client) {
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
        typesMap.forEach((k, v) -> builder.addMapping(k, MappingUtils.getMappingBuilder(k, v)));
        return builder.get().isAcknowledged();
    }

    protected boolean updateIndex(String index, Map<String, Class<? extends EsEntity>> typesMap) {
        boolean result = true;
        for (Map.Entry<String, Class<? extends EsEntity>> entry : typesMap.entrySet()) {
            result &= updateType(index, entry.getKey(), entry.getValue());
        }
        return result;
    }

    protected boolean updateType(String index, String type, Class<? extends EsEntity> model) {
        return client.admin().indices().preparePutMapping(index).setType(type)
                .setSource(MappingUtils.getMappingBuilder(type, model)).get().isAcknowledged();
    }

}
