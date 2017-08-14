import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import java.util.Date;

public class DateCell2 {

    @PolyImmutable Date getWhatever(@Readonly DateCell2 this) {
        return new @PolyImmutable Date();// In the T-NEW of updated version, instantiation to @PolyImmutable object is allowed
    }

    void typecheckInReImButNotTypeCheckIfAdaptedToReceiver(@Readonly DateCell2 this) {
        // Doesn't typecheck if adapted to receiver because @Readonly |> @PolyImmutable <: @Mutable is false.
        // But it typechecks in ReIm because @Mutable |> @PolyImmutable <: @Mutable holds.
        // In fall term 2016, I said, we want to get @Mutable object from a @Readonly object(also @Immutable object). That's true.
        // For example, it's universal that a method can create a local object and return it to the caller, so it can be mutated.
        // But here, we are instantiating poly return type to mutable type from a readonly receiver. Doing so doesn't harm anything of course,
        // but it makes the type system not intuitive anymore. Intuitively, a poly something becomes anything that is used to access it, that
        // should be the receiver. If a programmer really wants to mutable the returned object, he/she should make the return type of getWhatever mutable.
        //:: error: (assignment.type.incompatible)
        @Mutable Date whatever = this.getWhatever();
        // The only benefit of adapting to lhs is that a poly return type method can be instantiated to any lhs types regardless
        // of the receiver the method is invoked on. But is that a good matter? It only confuses the user. So I think viewpoint
        // adaptation to receiver makes more sense than lhs.
    }
}