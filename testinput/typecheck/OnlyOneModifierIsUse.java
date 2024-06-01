package typecheck;

import qual.Readonly;
import qual.Mutable;
import qual.Immutable;

public class OnlyOneModifierIsUse {

    // :: error: (type.invalid.conflicting.annos)
    // :: error: (initialization.field.uninitialized)
    @Readonly @Immutable Object field;
    // :: error: (type.invalid.conflicting.annos)
    // :: error: (initialization.field.uninitialized)
    String @Readonly @Immutable [] array;
}
