import qual.Immutable;

import java.util.Date;

public class FieldAssignCase3 {

    Date d;
    void foo(FieldAssignCase3 this, @Immutable Date ad) {
        // :: fixable-error: (assignment.type.incompatible)
        this.d = ad;
    }
}
