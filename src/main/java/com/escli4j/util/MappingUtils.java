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
            contentBuilder.field("properties");
            addPropertiesObject(contentBuilder, model);
            return contentBuilder.endObject().endObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void addObject(XContentBuilder contentBuilder, Datatype datatype, Class<?> model) {
        if (Datatype.OBJECT != datatype && Datatype.NESTED != datatype) {
            throw new IllegalArgumentException(
                    "Method getObjectBuiler allowed just for " + Datatype.OBJECT + " or " + Datatype.NESTED);
        }
        try {
            contentBuilder.startObject().field("type", datatype.name().toLowerCase());
            contentBuilder.field("properties");
            addPropertiesObject(contentBuilder, model);
            contentBuilder.endObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void addPropertiesObject(XContentBuilder contentBuilder, Class<?> model) throws IOException {
        contentBuilder.startObject();
        for (Field field : getAllAnnotatedFields(model, com.escli4j.annotations.Field.class)) {
            com.escli4j.annotations.Field fieldAnnotation = field.getAnnotation(com.escli4j.annotations.Field.class);
            String name = field.getName();
            Datatype datatype = fieldAnnotation.datatype();
            if (Datatype.NONE == datatype) {
                // skip
            } else if (Datatype.OBJECT == datatype || Datatype.NESTED == datatype) {
                // add object properties
                contentBuilder.field(name);
                addObject(contentBuilder, datatype, field.getType());
            } else {
                // add simple fields
                contentBuilder.startObject(name).field("type", datatype.name().toLowerCase()).endObject();
            }
        }
        contentBuilder.endObject();
    }

}
