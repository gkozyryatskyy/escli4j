package com.escli4j.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

public class MappingReflectUtils {
    
    public static Set<Class<?>> getAnnotatedClasses(String modelPackage, Class<? extends Annotation> annotation) {
        Reflections reflections;
        if (modelPackage != null) {
            new ConfigurationBuilder();
            reflections = new Reflections(ConfigurationBuilder.build(modelPackage));
        } else {
            reflections = new Reflections(ConfigurationBuilder.build(""));
        }
        return reflections.getTypesAnnotatedWith(annotation, true);
    }

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

}
