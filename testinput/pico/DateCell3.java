import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import java.util.Date;

public class DateCell3 {

    @PolyImmutable Date pimdate;
    @Immutable Date imdate;
    @Readonly Date rodate;

    @Readonly Date getAnother(@PolyImmutable DateCell3 this) {
        return this.rodate;
    }


    @Mutable Date getSecond(@Mutable DateCell3 this) {
        return this.pimdate;
    }


    @Immutable Date getThird(@PolyImmutable DateCell3 this) {
        return this.imdate;
    }


    void twoAdaptationsAreEquivalent1(@Mutable DateCell3 this) {
        // ReIm
        // q(this) <: @Readonly
        // @Readonly <: @Readonly

        // POI
        // q(this) <: q(this) |> @Poly
        // q(this) |> @Readonly <: @Readonly
        // Two adaptations are equivalant: "this" should be subtype of @Readonly
        // So @Mutable this makes sense in both adaptations
        @Readonly Date rd = this.getAnother();
    }

    void twoAdaptationsAreEquivalent2(@Mutable DateCell3 this) {
        // ReIm
        // q(this) <: @Mutable |> @Mutable = @sMutable
        // @Mutable |> @Mutable <: @Mutable

        // POI
        // q(this) <: q(this) |> @Mutable = @Mutable
        // q(this) |> @Mutable <: @Mutable
        // Two adaptations are equivalant: "this" should be @Mutable
        @Mutable Date md = this.getSecond();
    }

    void adaptationToLhsMakesNoSense(@Mutable DateCell3 this) {
        // ReIm
        // q(this) <: @Immutable
        // @Immutable <: @Immutable

        // POI
        // q(this) <: q(this) |> @Poly
        // q(this) |> @Immutable <: @Immutable

        // Adaptation to lhs unreasonably requires "this" to be @Immutable to invoke method
        // getThird(), which doesn't make sense. It's correct to invoke a method which returns
        // immutable object on @Mutable, @PolyImmutable, @Readonly and @Immutable objects.
        // Only adaptation to receiver allows this invocation. If we use lhs adaptation,
        // we will get a false positive warning that makes no sense.
        @Immutable Date imd = this.getThird();
    }
}