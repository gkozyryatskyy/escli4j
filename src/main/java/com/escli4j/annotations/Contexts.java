package com.escli4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Contexts{

    /**
     * contexts parameter support https://www.elastic.co/guide/en/elasticsearch/reference/current/suggester-context.html
     * for completion data type
     * @return contexts array
     */
    Context[] value();

}
