package com.escli4j.mapping;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

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
            buildDataType(contentBuilder, fieldType, fieldAnnotation.dataType(), field.getName());
            // ------------ process docValues -----------------
            buildDocValues(contentBuilder, fieldAnnotation.docValues());
            // ------------ process fields -----------------
            buildFields(contentBuilder, fieldType, fieldAnnotation.fields());
            contentBuilder.endObject();
        }
        contentBuilder.endObject();
    }

    private static void buildDataType(XContentBuilder contentBuilder, Class<?> javaType, DataType dataType, String name)
            throws IOException {
        // map data type
        if (DataType.NONE == dataType) {
            // skip
        } else if (DataType.OBJECT == dataType || DataType.NESTED == dataType) {
            // add object properties
            contentBuilder.field("type", dataType.name().toLowerCase());
            buildProperties(contentBuilder, javaType);
        } else {
            // add simple fields
            // TODO
            contentBuilder.field("type", dataType.name().toLowerCase());
        }
    }

    private static void buildDocValues(XContentBuilder contentBuilder, boolean docValues) throws IOException {
        if (!docValues) {
            contentBuilder.field("doc_values", false);
        }
    }

    private static void buildFields(XContentBuilder contentBuilder, Class<?> javaType, InnerField[] innerFields)
            throws IOException {
        if (innerFields.length > 0) {
            contentBuilder.startObject("fields");
            for (InnerField innerField : innerFields) {
                contentBuilder.startObject(innerField.name());
                // ------------ process dataType -----------------
                buildDataType(contentBuilder, javaType, innerField.dataType(), innerField.name());
                // ------------ process docValues -----------------
                buildDocValues(contentBuilder, innerField.docValues());
                contentBuilder.endObject();
            }
            contentBuilder.endObject();
        }
    }

}
