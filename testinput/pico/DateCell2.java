import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import java.util.Date;

//:: error: (initialization.fields.uninitialized)
public class DateCell2 {
    @Immutable Date imdate;

    @Immutable Date getImmutableDate(@PolyImmutable DateCell2 this) {
        return this.imdate;
    }

    /*Not allowed in ReIm. But allowed in PICO*/
    void test1(@Mutable DateCell2 this) {
        @Immutable Date imd = this.getImmutableDate();
    }

    void test2(@Immutable DateCell2 this) {
        @Immutable Date imd = this.getImmutableDate();
    }
}
