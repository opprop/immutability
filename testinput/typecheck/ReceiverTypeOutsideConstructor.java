import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

import java.util.Date;

// Immutable class
@Immutable
class A {
    @ReceiverDependantMutable Date d = new @Immutable Date();

    {
        // Bound annotation applies to "this" perfectly now. So no need to take action.
        d = new @Immutable Date();
    }

    @Immutable A() {
        d = new @Immutable Date();
    }

    // :: error: (constructor.return.incompatible)
    @ReceiverDependantMutable A(Object o1) {
        d = new @ReceiverDependantMutable Date();
    }

    // :: error: (constructor.return.incompatible) :: error: (type.invalid)
    @Mutable A(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

// :: error: (super.constructor.invocation.incompatible) :: error: (constructor.return.incompatible) :: error: (type.invalid)
@Immutable class AIMS extends A {}

// :: error: (subclass.bound.incompatible)
@ReceiverDependantMutable class ARDMS extends A {}

// :: error: (subclass.bound.incompatible)
@Mutable class AMS extends A {}

// :: error: (subclass.bound.incompatible)
class AUNKS extends A {}

// ReceiverDependantMutable class
@ReceiverDependantMutable
class B {
    @ReceiverDependantMutable Date d = new @ReceiverDependantMutable Date();

    {
        // Bound annotation applies to "this" perfectly now. So no need to take action.
        d = new @ReceiverDependantMutable Date();
    }

    // ok
    @Immutable B() {
        d = new @Immutable Date();
    }

    // ok
    @ReceiverDependantMutable B(Object o1) {
        d = new @ReceiverDependantMutable Date();
    }

    // ok
    @Mutable B(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

// :: error: (super.constructor.invocation.incompatible) :: error: (constructor.return.incompatible) :: error: (type.invalid)
@Immutable class BIMS extends B {}

// :: error: (super.constructor.invocation.incompatible)
@ReceiverDependantMutable class BRDMS extends B {}

// :: error: (super.constructor.invocation.incompatible)
@Mutable class BMS extends B {}

// mutable by default(TODO Does this make sense compared to defaulting to receiver-dependant-mutable?)
// :: error: (super.constructor.invocation.incompatible)
class BUNKS extends B {}

// Mutable class
@Mutable
class C {
    @ReceiverDependantMutable Date d = new @Mutable Date();

    {
        // Bound annotation applies to "this" perfectly now. So no need to take action.
        d = new @Mutable Date();
    }

    // :: error: (constructor.return.incompatible) :: error: (type.invalid)
    @Immutable C() {
        d = new @Immutable Date();
    }

    // :: error: (constructor.return.incompatible)
    @ReceiverDependantMutable C(Object o1) {
        d = new @ReceiverDependantMutable Date();
    }

    @Mutable C(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

// :: error: (subclass.bound.incompatible)
@Immutable class CIMS extends C {}

// :: error: (subclass.bound.incompatible)
@ReceiverDependantMutable class CRDMS extends C {}

// :: error: (super.constructor.invocation.incompatible)
@Mutable class CMS extends C {}

// :: error: (super.constructor.invocation.incompatible)
class CUNKS extends C {}

class D {
    @ReceiverDependantMutable Date d = new @Mutable Date();

    {
        d = new @Mutable Date();
    }
}

@Immutable
interface E {
    void foo(@Immutable E this);
}

// :: error: (constructor.return.incompatible) :: error: (type.invalid)
@Immutable public class ReceiverTypeOutsideConstructor implements E{
    @Override
    public void foo(@Immutable ReceiverTypeOutsideConstructor this) {

    }
}
