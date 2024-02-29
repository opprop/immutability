// Reason of Change: defaulting difference.

import qual.*;

public class ReadOnlyObject {
    // PICO defaults the return type of RDM class to mutable.
    // But String.valueOf(1) returns immutable.
    public @Immutable Object foo() {
        Object cat = null;
        return true ? String.valueOf(1) : cat;
    }

}