import qual.Mutable;
import qual.Immutable;
import qual.PolyImmutable;
import qual.Readonly;

public class FieldAssignment {

    @PolyImmutable Object f;

    void setFWithMutableReceiver(@Mutable FieldAssignment this, @Mutable Object o) {
        this.f = new @Mutable Object();
    }

    public FieldAssignment() {
        setFWithMutableReceiver(new @Mutable Object());
    }

    void setFWithPolyImmutableReceiver(@PolyImmutable FieldAssignment this, @PolyImmutable Object pimo) {
        //:: error: (illegal.write)
        this.f = pimo;
    }

    void setFWithImmutableReceiver(@Immutable FieldAssignment this, @Immutable Object imo) {
        //:: error: (illegal.write)
        this.f = imo;
    }
}