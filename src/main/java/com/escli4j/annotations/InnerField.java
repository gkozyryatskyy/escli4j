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
     * @return data type
     */
    DataType dataType() default DataType.NONE;

    /**
     * doc_values parameter support https://www.elastic.co/guide/en/elasticsearch/reference/current/doc-values.html
     * @return data type
     */
    boolean docValues() default true;

    /**
     * Name of the inner field. Default is not send, elasticsearch will creates it dynamically
     * @return data type
     */
    String name() default "";

}
