import qual.Immutable;
import qual.Readonly;
import qual.Mutable;
import qual.PolyImmutable;

public class ReadonlyNotCaptureMutable {
    static @Mutable Object smf = new @Mutable Object();

    @Readonly Object rof;
    @PolyImmutable ReadonlyNotCaptureMutable() {
        //:: error: (readonly.capture.mutable)
        rof = smf;
    }

    @Immutable ReadonlyNotCaptureMutable(@Immutable Object o) {
        //:: error: (readonly.capture.mutable)
        rof = smf;
    }

    @Mutable ReadonlyNotCaptureMutable(Object o1, Object o2) {
        rof = smf;
    }
}
