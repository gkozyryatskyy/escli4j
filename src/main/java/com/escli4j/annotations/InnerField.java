package com.escli4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.escli4j.mapping.DataType;

@Inherited
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InnerField{

    /**
     * Data type of the field. Default is not send, elasticsearch will creates it dynamically
     * @return dataType
     */
    DataType dataType() default DataType.NONE;

    /**
     * Doc_values parameter support https://www.elastic.co/guide/en/elasticsearch/reference/current/doc-values.html
     * @return docValues
     */
    boolean docValues() default true;

    /**
     * fielddata parameter support
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/fielddata.html#fielddata
     * @return fielddata
     */
    boolean fielddata() default false;

    /**
     * Analyzer parameter support https://www.elastic.co/guide/en/elasticsearch/reference/current/analyzer.html
     * @return analyzer
     */
    String analyzer() default "";

    /**
     * Search_analyzer parameter support
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-analyzer.html
     * @return search_analyzer
     */
    String search_analyzer() default "";

    /**
     * Name of the inner field. Default is not send, elasticsearch will creates it dynamically
     * @return name
     */
    String name() default "";

}
