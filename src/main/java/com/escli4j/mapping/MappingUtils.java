package com.escli4j.mapping;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.escli4j.annotations.Context;
import com.escli4j.annotations.Contexts;
import com.escli4j.annotations.InnerField;

public class MappingUtils {

    public static XContentBuilder getMappingBuilder(String type, Class<?> model) {
        return getMappingBuilder(type, null, model);
    }

    public static XContentBuilder getMappingBuilder(String type, String parent, Class<?> model) {
        try {
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder().startObject().startObject(type);
            if (parent != null && !"".equals(parent)) {
                contentBuilder.startObject("_parent").field("type", parent).endObject();
            }
            buildProperties(contentBuilder, model);
            return contentBuilder.endObject().endObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void buildProperties(XContentBuilder contentBuilder, Class<?> model) throws IOException {
        contentBuilder.startObject("properties");
        for (Field field : MappingReflectUtils.getAllAnnotatedFields(model, com.escli4j.annotations.Field.class)) {

            // unwrap
            Class<?> fieldType = field.getType();
            if (fieldType.getComponentType() != null) {
                // unwrap objects wrapped in arrays
                fieldType = fieldType.getComponentType();
            } else if (List.class.equals(fieldType)) {
                // unwrap objects wrapped in list
                fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            }

            // mapping fields
            com.escli4j.annotations.Field fieldAnnotation = field.getAnnotation(com.escli4j.annotations.Field.class);
            contentBuilder.startObject(field.getName());
            // ------------ process dataType -----------------
            buildType(contentBuilder, field, fieldType, fieldAnnotation.dataType(), field.getName());
            // ------------ process docValues -----------------
            buildDocValues(contentBuilder, fieldAnnotation.docValues());
            // ------------ process index -----------------
            buildIndex(contentBuilder, fieldAnnotation.index());
            // ------------ process fields -----------------
            buildFields(contentBuilder, field, fieldType, fieldAnnotation.fields());
            contentBuilder.endObject();
        }
        contentBuilder.endObject();
    }

    private static void buildType(XContentBuilder contentBuilder, Field field, Class<?> javaType, DataType dataType,
            String name) throws IOException {
        // map data type
        if (DataType.NONE == dataType) {
            // skip
        } else if (DataType.OBJECT == dataType || DataType.NESTED == dataType) {
            // add object properties
            contentBuilder.field("type", dataType.name().toLowerCase());
            buildProperties(contentBuilder, javaType);
        } else if (DataType.COMPLETION == dataType) {
            // add completion type
            contentBuilder.field("type", dataType.name().toLowerCase());
            Contexts contexts = field.getAnnotation(Contexts.class);
            if (contexts != null) {
                buildContexts(contentBuilder, field.getAnnotation(Contexts.class));
            }
        } else {
            // add simple fields
            contentBuilder.field("type", dataType.name().toLowerCase());
        }
    }

    private static void buildContexts(XContentBuilder contentBuilder, Contexts annotation) throws IOException {
        contentBuilder.startObject("context");
        for (Context context : annotation.value()) {
            buildContext(contentBuilder, context);
        }
        contentBuilder.endObject();
    }

    private static void buildContext(XContentBuilder contentBuilder, Context annotation) throws IOException {
        contentBuilder.startObject(annotation.name());
        contentBuilder.field("type", annotation.type().name().toLowerCase());
        contentBuilder.field("path", annotation.path());
        contentBuilder.endObject();
    }

    private static void buildDocValues(XContentBuilder contentBuilder, boolean docValues) throws IOException {
        if (!docValues) {
            contentBuilder.field("doc_values", false);
        }
    }
    
    private static void buildIndex(XContentBuilder contentBuilder, Index index) throws IOException {
        if (Index.NONE != index) {
            contentBuilder.field("index", index.name().toLowerCase());
        }
    }

    private static void buildFields(XContentBuilder contentBuilder, Field field, Class<?> javaType,
            InnerField[] innerFields) throws IOException {
        if (innerFields.length > 0) {
            contentBuilder.startObject("fields");
            for (InnerField innerField : innerFields) {
                contentBuilder.startObject(innerField.name());
                // ------------ process dataType -----------------
                buildType(contentBuilder, field, javaType, innerField.dataType(), innerField.name());
                // ------------ process docValues -----------------
                buildDocValues(contentBuilder, innerField.docValues());
                // ------------ process index -----------------
                buildIndex(contentBuilder, innerField.index());
                contentBuilder.endObject();
            }
            contentBuilder.endObject();
        }
    }

}
