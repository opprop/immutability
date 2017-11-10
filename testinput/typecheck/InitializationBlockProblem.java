package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

// TODO It's a shame that initialization block can't have annotations, so "this" parameter in
// initialization block are defaulted to @Mutable. This will break the type soundness if there
// the class has an @Immutable or @ReceiverDependantMutable constructor, because a @Mutable
// object can get @Immutable alias! :(
// TODO Even if we can annotate initialization blocks, we can't statically gurantee which constructor
// is going to be called. One possible way to solve this is to take least upper bound of all constructor
// return type in the class where the fields are declared and all the subclasses(because they also
// have access to the fields) and propagate that annotation to "this parameter" in initialization
// block, but that seems too cumbersome.
// TODO Maybe another way is to just forbid @ReceiverDependantMutable in initialization blocks(static
// initialization blocks don't have this problem because @ReceiverDependantMutable is forbidden anyway)
// Update after discussing with prof: constructors and helper methods that has explicit location to
// specify "this" type instead of using initialization blocks. In the existing initialization blocks,
// we just use @Readonly to be implicit type. And we need case studies to show how this approach works
// in the real world.
class InitializationBlockProblem {
    @ReceiverDependantMutable Object o;

    // Conservatively default "this" to be readonly, and do a case study
    {
        this.o = new @Mutable Object();
        // :: error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
