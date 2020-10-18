package jvn;

import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

//Interface d'annotation
public @interface JvnAnnotation {
    AnnotationType type();
}