import org.checkerframework.checker.initialization.qual.UnderInitialization;
import qual.Mutable;
import qual.Immutable;
import qual.PolyImmutable;
import qual.Readonly;

public class FieldAssignment {

    @PolyImmutable Object f;

    void setFWithMutableReceiver(@UnderInitialization @Mutable FieldAssignment this, @Mutable Object o) {
        this.f = new @Mutable Object();
    }

    // TODO This is not specific to PICO type system. Nullness also has this problem when we use instance
    // method to initialiaze fields
    //:: error: (initialization.fields.uninitialized)
    public FieldAssignment() {
        setFWithMutableReceiver(new @Mutable Object());
    }

    void setFWithPolyImmutableReceiver(@PolyImmutable FieldAssignment this, @PolyImmutable Object pimo) {
        //:: error: (illegal.field.write)
        this.f = pimo;
    }

    void setFWithImmutableReceiver(@Immutable FieldAssignment this, @Immutable Object imo) {
        //:: error: (illegal.field.write)
        this.f = imo;
    }
}