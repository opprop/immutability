package typecheck;

import qual.Readonly;
import qual.Mutable;
import qual.Immutable;

public class OnlyOneModifierIsUse {

    // :: error: (type.invalid.conflicting.annos)
    @Readonly @Immutable Object field;
    // :: error: (type.invalid.conflicting.annos)
    String @Readonly @Immutable [] array;
}
