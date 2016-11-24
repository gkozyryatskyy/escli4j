package com.escli4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.escli4j.mapping.Datatype;

@Inherited
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Field{

    /**
     * Name of the field. Default is not send, elasticsearch will creates it dynamically
     * @return data type
     */
    Datatype datatype() default Datatype.NONE;

}
