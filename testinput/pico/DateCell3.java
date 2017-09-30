import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;

import java.util.Date;

public class DateCell3{

    /*This poly return type can be instantiated according to assignment context now*/
    @PolyMutable Date getPolyMutableDate(@Readonly DateCell3 this) {
        return new @PolyMutable Date();
    }

    /*Allowed in new PICO now*/
    void testGetPolyImmutableDate(@Readonly DateCell3 this) {
        @Mutable Date md = this.getPolyMutableDate();
        @Immutable Date imd = this.getPolyMutableDate();
    }
}
