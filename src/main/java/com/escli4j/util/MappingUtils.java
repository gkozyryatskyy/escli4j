package com.escli4j.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import com.escli4j.Datatype;

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
            contentBuilder.field("properties", getPropertiesObjectBuiler(model).string());
            return contentBuilder.endObject().endObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static XContentBuilder getObjectBuiler(Datatype datatype, Class<?> model) {
        if (Datatype.OBJECT != datatype && Datatype.NESTED != datatype) {
            throw new IllegalArgumentException(
                    "Method getObjectBuiler allowed just for " + Datatype.OBJECT + " or " + Datatype.NESTED);
        }
        try {
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder().startObject().field("type",
                    datatype.name().toLowerCase());
            contentBuilder.field("properties", getPropertiesObjectBuiler(model).string());
            return contentBuilder.endObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static XContentBuilder getPropertiesObjectBuiler(Class<?> model) throws IOException {
        XContentBuilder contentBuilder = XContentFactory.jsonBuilder().startObject();
        for (Field field : getAllAnnotatedFields(model, com.escli4j.annotations.Field.class)) {
            com.escli4j.annotations.Field fieldAnnotation = field.getAnnotation(com.escli4j.annotations.Field.class);
            String name = field.getName();
            Datatype datatype = fieldAnnotation.datatype();
            if (Datatype.NONE == datatype) {
                // skip
            } else if (Datatype.OBJECT == datatype || Datatype.NESTED == datatype) {
                // add object properties
                contentBuilder.field(name, getObjectBuiler(datatype, field.getType()).string());
            } else {
                // add simple fields
                contentBuilder.startObject(name).field("type", datatype.name().toLowerCase()).endObject();
            }
        }
        return contentBuilder.endObject();
    }

}
