package typecheck;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import qual.Mutable;
import qual.Immutable;
import qual.ReceiverDependentMutable;

@ReceiverDependentMutable
public class FieldAssignment {

    @ReceiverDependentMutable Object f;

    void setFWithMutableReceiver(@UnderInitialization @Mutable FieldAssignment this, @Mutable Object o) {
        this.f = new @Mutable Object();
    }

    // TODO This is not specific to PICO type system. InitializationVisitor currently has this issue of false positively
    // wanrning uninitialized fields when we use instance method to initialiaze fields
    public FieldAssignment() {
        // :: error: (method.invocation.invalid)
        setFWithMutableReceiver(new @Mutable Object());
    }

    void setFWithReceiverDependentMutableReceiver(@ReceiverDependentMutable FieldAssignment this, @ReceiverDependentMutable Object pimo) {
        // :: error: (illegal.field.write)
        this.f = pimo;
    }

    void setFWithImmutableReceiver(@Immutable FieldAssignment this, @Immutable Object imo) {
        // :: error: (illegal.field.write)
        this.f = imo;
    }
}
