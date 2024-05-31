import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

public class FieldInitializer {
    // :: fixable-error: (assignment.type.incompatible)
    @ReceiverDependentMutable Object o = new @Immutable Object();
}

class A {
    @ReceiverDependentMutable Object o = new @Mutable Object();
}

class B {
    // :: fixable-error: (assignment.type.incompatible)
    @ReceiverDependentMutable Object o = new @ReceiverDependentMutable Object();
}

@Immutable
class C {
    @Mutable Object o = new Object();
}

@ReceiverDependentMutable
class D {
    @Mutable Object o = new Object();
}
