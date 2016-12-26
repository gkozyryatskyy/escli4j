package com.escli4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// used for completion data type
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Context{

    public static enum ContextType {

        CATEGORY, GEO
    }

    /**
     * Name of the context
     * @return name of the context
     */
    String name();

    /**
     * Type of the context
     * @return type of the context
     */
    ContextType type() default ContextType.CATEGORY;

    /**
     * Path for field in the document. If path is defined then the context type values are read from that path in the
     * document
     * @return data type
     */
    String path();

}
