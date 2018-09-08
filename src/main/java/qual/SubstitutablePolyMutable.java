package qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@PolymorphicQualifier(Readonly.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface SubstitutablePolyMutable {}
