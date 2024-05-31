import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

import java.util.Date;

// Immutable class
@Immutable
class A {
    @ReceiverDependentMutable Date d = new @Immutable Date();

    {
        // Bound annotation applies to "this" perfectly now. So no need to take action.
        d = new @Immutable Date();
    }

    @Immutable A() {
        d = new @Immutable Date();
    }

    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentMutable A(Object o1) {
        d = new @ReceiverDependentMutable Date();
    }

    // :: error: (type.invalid.annotations.on.use)
    @Mutable A(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

@Immutable class AIMS extends A {}

// :: error: (type.invalid.annotations.on.use) :: error: (super.invocation.invalid)
@ReceiverDependentMutable class ARDMS extends A {}

// :: error: (type.invalid.annotations.on.use) :: error: (super.invocation.invalid)
@Mutable class AMS extends A {}

// :: error: (type.invalid.annotations.on.use) :: error: (super.invocation.invalid)
class AUNKS extends A {}

// ReceiverDependentMutable class
@ReceiverDependentMutable
class B {
    @ReceiverDependentMutable Date d = new @ReceiverDependentMutable Date();

    {
        // Bound annotation applies to "this" perfectly now. So no need to take action.
        d = new @ReceiverDependentMutable Date();
    }

    // ok
    @Immutable B() {
        d = new @Immutable Date();
    }

    // ok
    @ReceiverDependentMutable B(Object o1) {
        d = new @ReceiverDependentMutable Date();
    }

    // ok
    @Mutable B(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

@Immutable class BIMS extends B {}

// :: error: (super.invocation.invalid)
@ReceiverDependentMutable class BRDMS extends B {}

// :: error: (super.invocation.invalid)
@Mutable class BMS extends B {}

// mutable by default(TODO Does this make sense compared to defaulting to receiver-dependant-mutable?)
// :: error: (super.invocation.invalid)
class BUNKS extends B {}

// Mutable class
@Mutable
class C {
    @ReceiverDependentMutable Date d = new @Mutable Date();

    {
        // Bound annotation applies to "this" perfectly now. So no need to take action.
        d = new @Mutable Date();
    }

    // :: error: (type.invalid.annotations.on.use)
    @Immutable C() {
        d = new @Immutable Date();
    }

    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentMutable C(Object o1) {
        d = new @ReceiverDependentMutable Date();
    }

    @Mutable C(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

// :: error: (type.invalid.annotations.on.use)
@Immutable class CIMS extends C {}

// :: error: (type.invalid.annotations.on.use) :: error: (super.invocation.invalid)
@ReceiverDependentMutable class CRDMS extends C {}

// :: error: (super.invocation.invalid)
@Mutable class CMS extends C {}

// :: error: (super.invocation.invalid)
class CUNKS extends C {}

class D {
    @ReceiverDependentMutable Date d = new @Mutable Date();

    {
        d = new @Mutable Date();
    }
}

@Immutable
interface E {
    void foo(@Immutable E this);
}

@Immutable public class ReceiverTypeOutsideConstructor implements E{
    @Override
    public void foo(@Immutable ReceiverTypeOutsideConstructor this) {

    }
}
