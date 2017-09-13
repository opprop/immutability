import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import java.util.Date;

public class DateCell3{

    /*This poly return type can be instantiated according to assignment context now*/
    @PolyImmutable Date getPolyImmutableDate(@Readonly DateCell3 this) {
        return new @PolyImmutable Date();
    }

    /*Allowed in new PICO now*/
    void testGetPolyImmutableDate(@Readonly DateCell3 this) {
        @Mutable Date md = this.getPolyImmutableDate();
        @Immutable Date imd = this.getPolyImmutableDate();
    }
}
