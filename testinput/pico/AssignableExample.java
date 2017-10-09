import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.Assignable;

import java.util.Date;

public class AssignableExample {
    @Immutable Object o;
    @Immutable Date date;
    @Assignable @Immutable Date assignableDate;

    @Mutable AssignableExample() {
        o = new @Immutable Object();
    }

    void foo(@Immutable AssignableExample this) {
        //:: error: (illegal.field.write)
        this.date = new @Immutable Date();
        this.assignableDate = new @Immutable Date();
    }

    void foo2(@Mutable AssignableExample this) {
        this.o = new @Immutable Object();
    }
}
class Subclass extends AssignableExample {
    void bar(@Immutable Subclass this) {
        //:: error: (illegal.field.write)
        this.date = new @Immutable Date();
        this.assignableDate = new @Immutable Date();
    }

    void bar2(@Mutable Subclass this) {
        this.date = new @Immutable Date();
        this.assignableDate = new @Immutable Date();
    }
}
