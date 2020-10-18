package jvn;

import java.lang.annotation.*;
import java.lang.reflect.Method;
//Classe reader des annotations
public class JvnGetAnnotation {

    public AnnotationType getFormat(Method m) {
        //Vérification de la présence des annotations
        if (m.isAnnotationPresent(JvnAnnotation.class)) {
        // Get a reference on the annotation
            JvnAnnotation readAnno = m.getAnnotation(JvnAnnotation.class);
            // Get the annotation value
            if(readAnno.type() == AnnotationType.READ){
                return AnnotationType.READ;
            }else {
                return AnnotationType.WRITE;
            }
        }else {
            return null;
        }
    }
}
