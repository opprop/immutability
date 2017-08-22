import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import java.util.Date;

public class DateCell3{

    /*If this is a polyimmutable receiver reads polyimmutable field, then this wouldn't be an issue*/
    /*Original purpose of instantiating polyimmutable object is to initialize polyimmutable field in polyimmutable constructor*/
    @PolyImmutable Date getPolyImmutableDate(@Readonly DateCell3 this) {
        return new @PolyImmutable Date();
    }

    /**Not allowed in PICO**/
    void lhsMakeSense(@Readonly DateCell3 this) {
        //:: error: (assignment.type.incompatible)
        @Mutable Date whatever = this.getPolyImmutableDate();
         /*The benefit of adapting to lhs is that a poly return type method can be instantiated
         to any lhs types. Any example type system that has similiar hierarchy and has this
         powerfull polymorphism? How about GUT?*/
    }
}
