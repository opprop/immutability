import qual.Mutable;
import qual.ReceiverDependentMutable;
import qual.Immutable;

@Immutable class RDMAllowedAsMethodReceiver {
    // :: error: (type.invalid.annotations.on.use) :: error: (method.receiver.incompatible)
    void foo(@ReceiverDependentMutable RDMAllowedAsMethodReceiver this) {}
}

@Mutable
class AnotherExample {
    // :: error: (type.invalid.annotations.on.use) :: error: (method.receiver.incompatible)
    void foo(@ReceiverDependentMutable AnotherExample this) {}
}
