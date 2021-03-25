import qual.Immutable;

import java.lang.Cloneable;

@Immutable interface IIIC_ImmutableInterface {
}

// The use of IIIC_ImmutableInterface defaulted to @Mutable.
// So the implement action is valid for the class (implementing a mutable interface)
// But for the type use of IIIC_ImmutableInterface it is invalid.
// :: error: type.invalid.annotations.on.use
public class InvalidImmutableInterfaceClass implements Cloneable, IIIC_ImmutableInterface {

}