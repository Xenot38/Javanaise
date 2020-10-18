package irc;
import jvn.AnnotationType;
import jvn.JvnGetAnnotation;
import jvn.JvnAnnotation;
public interface SentenceItf {

    @JvnAnnotation( type = AnnotationType.WRITE)
    public void write(String text);
    @JvnAnnotation( type = AnnotationType.READ)
    public String read();
}
