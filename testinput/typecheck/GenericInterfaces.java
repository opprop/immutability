import qual.Immutable;
import qual.Mutable;
import qual.Readonly;

interface MIt<E extends @Readonly Object> {
    E next();
}

class GenericInterfaces {

    void raw() {
        @Mutable MIt raw = null;

        // Using optimictic uninferred type arguments, so it is
        // allowed
        @Immutable Object ro = raw.next();
    }
}
