package day.happy365.shorturlservice.config;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {
    String value();
}
