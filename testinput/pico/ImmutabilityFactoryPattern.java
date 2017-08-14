import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

public class ImmutabilityFactoryPattern {
    public @Immutable ImmutabilityFactoryPattern() {

    }

    @PolyImmutable Object createObject(@Readonly ImmutabilityFactoryPattern this) {
        return new @PolyImmutable Object();
    }

    static void test() {
        @Immutable ImmutabilityFactoryPattern factory = new ImmutabilityFactoryPattern();
        // It seems like ReIm is more flexible. But current CF doesn't support only one
        // polymorphic annotation on method return type.
        //:: error: (assignment.type.incompatible)
        @Mutable Object mo = factory.createObject();// Typecheck in ReIm.
        @Immutable Object imo = factory.createObject();// Typecheck in both
    }
}