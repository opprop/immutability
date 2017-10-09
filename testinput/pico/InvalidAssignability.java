import qual.Assignable;
import qual.Immutable;

public class InvalidAssignability {
    final @Immutable Object io = null;
    @Immutable Object io2;
    @Assignable @Immutable Object io3;
    //:: error: (one.assignability.invalid)
    final @Assignable @Immutable Object o = null;
}
