import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

@Immutable
public class ViewpointAdaptToBound {
    // :: fixable-error: (assignment.type.incompatible)
    @ReceiverDependantMutable Object o = new Object();
    // :: fixable-error: (assignment.type.incompatible)
    @Immutable Object x = new Object();
    @Mutable Object y = new Object();

    {
        // :: fixable-error: (assignment.type.incompatible)
        o = new Object();
    }
}

@Mutable
class A {
    @ReceiverDependantMutable Object o = new Object();
    // :: fixable-error: (assignment.type.incompatible)
    @Immutable Object x = new Object();
    @Mutable Object y = new Object();

    {
        o = new Object();
    }
}

@ReceiverDependantMutable
class B {
    // :: fixable-error: (assignment.type.incompatible)
    @ReceiverDependantMutable Object o = new Object();
    // :: fixable-error: (assignment.type.incompatible)
    @Immutable Object x = new Object();
    @Mutable Object y = new Object();

    {
        // :: fixable-error: (assignment.type.incompatible)
        o = new Object();
    }
}
