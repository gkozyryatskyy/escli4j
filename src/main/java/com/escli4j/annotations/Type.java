package com.escli4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Type{

    /**
     * Name of the elasticsearch index
     * @return index name
     */
    String index();

    /**
     * Name of the elasticsearch type
     * @return type name
     */
    String type();

    /**
     * Name of the parent type of this type
     * @return type name
     */
    String parent() default "";

    /**
     * Create index and type if not exist
     * @return true if index should be created if not exists, false otherwise
     */
    boolean create() default true;

    /**
     * Update index and type if exist
     * @return true if index should be updates if exists, false otherwise
     */
    boolean update() default false;

}
