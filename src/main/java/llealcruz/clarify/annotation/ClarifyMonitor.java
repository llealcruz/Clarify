package llealcruz.clarify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClarifyMonitor {
    String tag() default "GENERAL"; // Utilizado para filtrar métodos

    String action() default ""; // Utilizado para tradução humanizada do método

    long warnMs() default 500; // ms para disparar aviso

    long dangerMs() default 1000; // ms para disparar perigo

    boolean recordExceptions() default false; // Se deve gerar um log quando o método falhar
}
