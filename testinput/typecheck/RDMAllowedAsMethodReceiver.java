import qual.Mutable;
import qual.ReceiverDependantMutable;
import qual.Immutable;

@Immutable class RDMAllowedAsMethodReceiver {
    // :: error: (type.invalid.annotations.on.use)
    void foo(@ReceiverDependantMutable RDMAllowedAsMethodReceiver this) {}
}

@Mutable
class AnotherExample {
    // :: error: (type.invalid.annotations.on.use)
    void foo(@ReceiverDependantMutable AnotherExample this) {}
}
