import qual.Immutable;

import java.util.Date;

@Immutable
class A {
    @Immutable A() {}
}

// @skip-test Fix PICOValidator
// Shouldn't get any errors, but it did
public class NewAnnonymousClass {
    protected static A funLUDecompose() {
        return new A() {
            Date d = new @Immutable Date();
        };
    }
}
