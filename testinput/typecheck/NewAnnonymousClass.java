import qual.Immutable;
import qual.ReceiverDependentMutable;

import java.util.Date;

@Immutable
class A {
    @Immutable A() {}
}

// @skip-test Until we can get bound annotation on the super type that an anonymous class
// inherits from
public class NewAnnonymousClass {
    protected static A funLUDecompose() {
        // Even though there is no explicit @Immutable annotation on new, new A() still gets
        // @Immutable type. Is this expected behaviour?
        return new A() {
            Date d = new @Immutable Date();
            // Below are not allowed by Java. Will get "Illegal static declaration in inner
            // class <anonymous NewAnnonymousClass$1>"
            // static @ReceiverDependentMutable Object o;
            // static {
            //    o = null;
            // }
            // static @ReceiverDependentMutable Object foo() {return null;}
        };
    }
}
