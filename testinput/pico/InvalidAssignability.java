import qual.Assignable;
import qual.Immutable;

public class InvalidAssignability {
    final @Immutable Object io = null;
    @Immutable Object io2;
    @Assignable @Immutable Object io3;
    static final @Immutable Object io4 = null;
    static @Assignable @Immutable Object io5;
    //:: error: (one.assignability.invalid)
    final @Assignable @Immutable Object o = null;
    //:: error: (one.assignability.invalid)
    static final @Assignable @Immutable Object o2 = null;
}
