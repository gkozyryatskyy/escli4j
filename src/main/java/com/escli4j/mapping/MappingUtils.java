package com.escli4j.mapping;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.escli4j.annotations.InnerField;

public class MappingUtils {

    public static List<Field> getAllAnnotatedFields(Class<?> type, Class<? extends Annotation> annotation) {
        List<Field> retval = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotation)) {
                retval.add(field);
            }
        }
        if (type.getSuperclass() != null) {
            retval.addAll(getAllAnnotatedFields(type.getSuperclass(), annotation));
        }
        return retval;
    }

    public static XContentBuilder getMappingBuilder(String type, Class<?> model) {
        try {
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder().startObject().startObject(type);
            buildPropertiesObject(contentBuilder, model);
            return contentBuilder.endObject().endObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // private static void buildObject(XContentBuilder contentBuilder, DataType datatype, Class<?> model) {
    // if (DataType.OBJECT != datatype && DataType.NESTED != datatype) {
    // throw new IllegalArgumentException(
    // "Method getObjectBuiler allowed just for " + DataType.OBJECT + " or " + DataType.NESTED);
    // }
    // try {
    // contentBuilder.startObject().field("type", datatype.name().toLowerCase());
    // buildPropertiesObject(contentBuilder, model);
    // contentBuilder.endObject();
    // } catch (IOException e) {
    // throw new IllegalStateException(e);
    // }
    // }

    private static void buildPropertiesObject(XContentBuilder contentBuilder, Class<?> model) throws IOException {
        contentBuilder.startObject("properties");
        for (Field field : getAllAnnotatedFields(model, com.escli4j.annotations.Field.class)) {
            com.escli4j.annotations.Field fieldAnnotation = field.getAnnotation(com.escli4j.annotations.Field.class);
            contentBuilder.startObject(field.getName());
            // ------------ process dataType -----------------
            buildDataType(contentBuilder, field.getType(), fieldAnnotation.dataType(), field.getName());
            // ------------ process docValues -----------------
            buildDocValues(contentBuilder, fieldAnnotation.docValues());
            // ------------ process fields -----------------
            buildFields(contentBuilder, field.getType(), fieldAnnotation.fields());
            contentBuilder.endObject();
        }
        contentBuilder.endObject();
    }

    private static void buildDataType(XContentBuilder contentBuilder, Class<?> javaType, DataType dataType, String name)
            throws IOException {
        if (DataType.NONE == dataType) {
            // skip
        } else if (DataType.OBJECT == dataType || DataType.NESTED == dataType) {
            // add object properties
            contentBuilder.field("type", dataType.name().toLowerCase());
            buildPropertiesObject(contentBuilder, javaType);
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
