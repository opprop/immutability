package qual;

import org.checkerframework.framework.qual.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SubtypeOf({Mutable.class, Immutable.class, PolyMutable.class, ReceiverDependantMutable.class})
@DefaultFor({ TypeUseLocation.LOWER_BOUND })
@Documented
@Retention(RetentionPolicy.RUNTIME)
// Stop allowing any explicit usage of @Bottom qualifier in source. As it causes difficulty to
// differentiate correct explicit usage of @Bottom and internally propagated @Bottom. Instead,
// if programmers don't write anything on explicit lower bound(of a wildcard), we still have
// defaulting mechanism to make the explicit lower bound to be @Bottom. They can still use other
// qualifier than @Bottom explicitly on explicit lower bound to have different-than-default type.
@Target({})
@TargetLocations({})
@DefaultInUncheckedCodeFor({TypeUseLocation.RETURN})
public @interface Bottom {}
