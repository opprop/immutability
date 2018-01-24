
import qual.Assignable;
import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

import java.util.Date;

class A {
    @Assignable Date d;
    A() {
        // :: fixable-error: (assignment.type.incompatible)
        d = new @ReceiverDependantMutable Date();
    }
}

public class RDMConstructor {
    void test1() {
        // :: fixable-error: (type.invalid.annotations.on.use)
        @Immutable A la = new A();
        la.toString();
    }

    void test2() {
        A la = new A();
        // :: fixable-error: (assignment.type.incompatible)
        la.d = new @Immutable Date();
    }
}
