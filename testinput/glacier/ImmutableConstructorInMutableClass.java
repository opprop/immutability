package glacier;

import qual.Immutable;



public class ImmutableConstructorInMutableClass {
    // :: error: (type.invalid.annotations.on.use)
    public @Immutable ImmutableConstructorInMutableClass() {
    }

    public void aMethod() {

    }
}