import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

public class FieldInitializer {
    // :: fixable-error: (assignment.type.incompatible)
    @ReceiverDependantMutable Object o = new @Immutable Object();
}

class A {
    @ReceiverDependantMutable Object o = new @Mutable Object();
}

class B {
    // :: fixable-error: (assignment.type.incompatible)
    @ReceiverDependantMutable Object o = new @ReceiverDependantMutable Object();
}

@Immutable
class C {
    @Mutable Object o = new Object();
}

@ReceiverDependantMutable
class D {
    @Mutable Object o = new Object();
}
