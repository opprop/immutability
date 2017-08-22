import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import java.util.Date;

//:: error: (initialization.fields.uninitialized)
public class DateCell2 {
    @Immutable Date imdate;

    /*CF states that polymorphic qualifier should be used at least twice.*/
    @Immutable Date getImmutableDate(@PolyImmutable DateCell2 this) {
        return this.imdate;
    }

    /**Not allowed in ReIm**/
    void receiverMakeSense(@Mutable DateCell2 this) {
        @Immutable Date imd = this.getImmutableDate();
    }
}
