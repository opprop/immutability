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

    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependantMutable A(Object o1) {
        d = new @ReceiverDependantMutable Date();
    }

    // :: error: (type.invalid.annotations.on.use)
    @Mutable A(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

@Immutable class AIMS extends A {}

// :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
@ReceiverDependantMutable class ARDMS extends A {}

// :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
@Mutable class AMS extends A {}

// :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
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

@Immutable class BIMS extends B {}

// :: error: (super.invocation.invalid)
@ReceiverDependantMutable class BRDMS extends B {}

// :: error: (super.invocation.invalid)
@Mutable class BMS extends B {}

// mutable by default(TODO Does this make sense compared to defaulting to receiver-dependant-mutable?)
// :: error: (super.invocation.invalid)
class BUNKS extends B {}

// Mutable class
@Mutable
class C {
    @ReceiverDependantMutable Date d = new @Mutable Date();

    {
        // Bound annotation applies to "this" perfectly now. So no need to take action.
        d = new @Mutable Date();
    }

    // :: error: (type.invalid.annotations.on.use)
    @Immutable C() {
        d = new @Immutable Date();
    }

    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependantMutable C(Object o1) {
        d = new @ReceiverDependantMutable Date();
    }

    @Mutable C(Object o1, Object o2) {
        d = new @Mutable Date();
    }
}

// :: error: (declaration.inconsistent.with.extends.clause)
@Immutable class CIMS extends C {}

// :: error: (declaration.inconsistent.with.extends.clause) :: error: (super.invocation.invalid)
@ReceiverDependantMutable class CRDMS extends C {}

// :: error: (super.invocation.invalid)
@Mutable class CMS extends C {}

// :: error: (super.invocation.invalid)
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

@Immutable public class ReceiverTypeOutsideConstructor implements E{
    @Override
    public void foo(@Immutable ReceiverTypeOutsideConstructor this) {

    }
}
