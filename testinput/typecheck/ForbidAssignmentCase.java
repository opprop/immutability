package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;
import qual.Assignable;

@ReceiverDependantMutable
public class ForbidAssignmentCase {
    @Assignable @ReceiverDependantMutable Object f;
    @ReceiverDependantMutable ForbidAssignmentCase() {
        f = new @ReceiverDependantMutable Object();
    }

    // Allowing assignment through @Readonly receiver to @Assignable @ReceiverDependantMutable
    // in either way causes errors. So I would forbid this combination in assignment. Otherwise,
    // we don't. For example, we still allow reading this field by @Readonly receiver
    static void forbid(@Readonly ForbidAssignmentCase fac) {
        // :: error: (illegal.field.write)
        fac.f = new @Immutable Object();
        Object lo = fac.f;
    }

    // Below are different cases. Because dataflow refinement refines @Readonly to concrete type,
    // so all the below don't hit the forbidden case
    static void ImmutableObjectCaptureMutableObject() {
        @Immutable ForbidAssignmentCase imo = new @Immutable ForbidAssignmentCase();
        @Readonly ForbidAssignmentCase ro = imo;
        // @Immutable object captures @Mutable object
        // :: error: (assignment.type.incompatible)
        ro.f = new @Mutable Object();
        // victim is no longer @Immutable object any more.
        @Immutable Object victim = imo.f;

        // But allow below:
        ro.f = new @Immutable Object();
    }

    static void ImmutableObjectGetMutableAlias() {
        @Mutable ForbidAssignmentCase mo = new @Mutable ForbidAssignmentCase();
        @Readonly ForbidAssignmentCase ro = mo;
        // :: error: (assignment.type.incompatible)
        ro.f = new @Immutable Object();
        // @Immutable object pointed by field f gets @Mutable alias
        @Mutable Object mutableAliasToImmutableObject = mo.f;

        // But allow below:
        ro.f = new @Mutable Object();
    }

    static @Mutable Object getMutableAliasForReadonlyArgument(@Readonly Object p) {
        @Mutable ForbidAssignmentCase mo = new @Mutable ForbidAssignmentCase();
        @Readonly ForbidAssignmentCase ro = mo;
        // :: error: (assignment.type.incompatible)
        ro.f = p;
        // Got a mutable alias for @Readonly p
        return mo.f;
    }

    static @Immutable Object getImmutableAliasForReadonlyArgument(@Readonly Object p) {
        @Immutable ForbidAssignmentCase imo = new @Immutable ForbidAssignmentCase();
        @Readonly ForbidAssignmentCase ro = imo;
        // :: error: (assignment.type.incompatible)
        ro.f = p;
        // Got an immutable alias for @Readonly p
        return imo.f;
    }
}
