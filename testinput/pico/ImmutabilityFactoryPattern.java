import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;

public class ImmutabilityFactoryPattern {
    public @Immutable ImmutabilityFactoryPattern() {

    }

    @PolyMutable Object createObject(@Readonly ImmutabilityFactoryPattern this) {
        return new @PolyMutable Object();
    }

    static void test() {
        @Immutable ImmutabilityFactoryPattern factory = new @Immutable ImmutabilityFactoryPattern();
        // Both typecheck in new PICO
        @Mutable Object mo = factory.createObject();
        @Immutable Object imo = factory.createObject();
    }
}
