package com.escli4j.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EdgeNGramFilter{
    
    String type = "edge_ngram";
    
    String name();
    
    int min_gram() default 1;
    
    int max_gram() default 20;

}
