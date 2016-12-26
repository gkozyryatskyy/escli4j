package com.escli4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.escli4j.mapping.DataType;
import com.escli4j.mapping.Index;

@Inherited
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Field{

    /**
     * Data type of the field. Default is not send, elasticsearch will creates it dynamically
     * @return data type
     */
    DataType dataType() default DataType.NONE;

    /**
     * doc_values parameter support https://www.elastic.co/guide/en/elasticsearch/reference/current/doc-values.html
     * @return data type
     */
    boolean docValues() default true;

    /**
     * index parameter support https://www.elastic.co/guide/en/elasticsearch/reference/2.3/mapping-index.html
     * @return data type
     */
    Index index() default Index.NONE;

    /**
     * fields parameter support https://www.elastic.co/guide/en/elasticsearch/reference/current/multi-fields.html
     * @return data type
     */
    InnerField[] fields() default {};

}
