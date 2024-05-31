package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.ReceiverDependentMutable;

import java.util.Date;

@ReceiverDependentMutable public class DateCell2 {
    // :: error: (initialization.field.uninitialized)
    @Immutable Date imdate;

    @Immutable Date getImmutableDate(@PolyMutable DateCell2 this) {
        return this.imdate;
    }

    /*Not allowed in ReIm. But allowed in PICO*/
    void test1(@Mutable DateCell2 this) {
        @Immutable Date imd = this.getImmutableDate();
    }

    void test2(@Immutable DateCell2 this) {
        @Immutable DateCell2 waht = new @Immutable DateCell2();
        @Immutable Date imd = this.getImmutableDate();
    }
}
