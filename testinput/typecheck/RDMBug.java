package typecheck;

import qual.Assignable;
import qual.Immutable;
import qual.Mutable;
import qual.Readonly;

// :: error: (initialization.fields.uninitialized)
@Immutable class RDMBug {
    @Mutable Object o;
    @Readonly Object o2;
    void foo(@Immutable RDMBug this) {
        // :: error: (illegal.field.write)
        this.o = new @Mutable Object();
        // :: error: (illegal.field.write)
        this.o2 = new @Immutable Object();
    }
}
