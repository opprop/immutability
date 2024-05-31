package qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by mier on 06/10/17.
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@HoldsForDefaultValue
public @interface Assignable {}
