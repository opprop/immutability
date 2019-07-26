package qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

@PolymorphicQualifier(Readonly.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface SubstitutablePolyMutable {}
