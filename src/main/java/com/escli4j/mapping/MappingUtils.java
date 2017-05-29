package com.escli4j.mapping;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.escli4j.annotations.Analyzers;
import com.escli4j.annotations.Context;
import com.escli4j.annotations.Contexts;
import com.escli4j.annotations.CustomAnalyzer;
import com.escli4j.annotations.EdgeNGramFilter;
import com.escli4j.annotations.InnerField;
import com.escli4j.mapping.model.AnalysisDto;
import com.escli4j.mapping.model.AnalyzerDto;
import com.escli4j.mapping.model.CustomAnalyzerDto;
import com.escli4j.mapping.model.EdgeNGramFilterDto;
import com.escli4j.mapping.model.FilterDto;
import com.escli4j.mapping.model.SettingsDto;
import com.escli4j.util.EscliJsonUtils;

public class MappingUtils {

    ///////////////////////////////// SETTINGS /////////////////////////////////

    public static String getSettingsBuilder(List<Annotation> annotations) {
        try {
            SettingsDto settings = new SettingsDto(
                    new AnalysisDto(buildFilters(annotations), buildAnalyzers(annotations)));
            if (settings.getAnalysis() == null) {
                return null;
            } else {
                return EscliJsonUtils.writeValueAsString(settings);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, FilterDto> buildFilters(List<Annotation> annotations) throws IOException {
        Map<String, FilterDto> retval = new HashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation instanceof EdgeNGramFilter) {
                EdgeNGramFilter edgeNGram = (EdgeNGramFilter) annotation;
                retval.put(edgeNGram.name(), new EdgeNGramFilterDto(edgeNGram));
            }
        }
        return retval;
    }

    private static Map<String, AnalyzerDto> buildAnalyzers(List<Annotation> annotations) throws IOException {
        Map<String, AnalyzerDto> retval = new HashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Analyzers) {
                for (CustomAnalyzer customAnalyzer : ((Analyzers) annotation).value()) {
                    retval.put(customAnalyzer.name(), new CustomAnalyzerDto(customAnalyzer));
                }
            } else if (annotation instanceof CustomAnalyzer) {
                CustomAnalyzer customAnalyzer = (CustomAnalyzer) annotation;
                retval.put(customAnalyzer.name(), new CustomAnalyzerDto(customAnalyzer));
            }
        }
        return retval;
    }

    ///////////////////////////////// MAPPINGS /////////////////////////////////

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
            // ------------ process fielddata -----------------
            buildFielddata(contentBuilder, fieldAnnotation.fielddata());
            // ------------ process analyzer -----------------
            buildAnalyzer(contentBuilder, fieldAnnotation.analyzer());
            // ------------ process search_analyzer -----------------
            buildSearchAnalyzer(contentBuilder, fieldAnnotation.search_analyzer());
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
        contentBuilder.startArray("contexts");
        for (Context context : annotation.value()) {
            buildContext(contentBuilder, context);
        }
        contentBuilder.endArray();
    }

    private static void buildContext(XContentBuilder contentBuilder, Context annotation) throws IOException {
        contentBuilder.startObject();
        contentBuilder.field("name", annotation.name());
        contentBuilder.field("type", annotation.type().name().toLowerCase());
        contentBuilder.field("path", annotation.path());
        contentBuilder.endObject();
    }

    private static void buildDocValues(XContentBuilder contentBuilder, boolean docValues) throws IOException {
        if (!docValues) {
            contentBuilder.field("doc_values", false);
        }
    }

    private static void buildFielddata(XContentBuilder contentBuilder, boolean fielddata) throws IOException {
        if (fielddata) {
            contentBuilder.field("fielddata", true);
        }
    }

    private static void buildAnalyzer(XContentBuilder contentBuilder, String analyzer) throws IOException {
        if (!"".equals(analyzer)) {
            contentBuilder.field("analyzer", analyzer);
        }
    }

    private static void buildSearchAnalyzer(XContentBuilder contentBuilder, String searchAnalyzer) throws IOException {
        if (!"".equals(searchAnalyzer)) {
            contentBuilder.field("search_analyzer", searchAnalyzer);
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
                // ------------ process doc_values -----------------
                buildDocValues(contentBuilder, innerField.docValues());
                // ------------ process fielddata -----------------
                buildFielddata(contentBuilder, innerField.fielddata());
                // ------------ process analyzer -----------------
                buildAnalyzer(contentBuilder, innerField.analyzer());
                // ------------ process search_analyzer -----------------
                buildSearchAnalyzer(contentBuilder, innerField.search_analyzer());
                contentBuilder.endObject();
            }
            contentBuilder.endObject();
        }
    }

}
