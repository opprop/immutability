import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;

import java.util.Date;

class Thief {
    @NotOnlyInitialized @PolyImmutable
    SuperClass2 victimCaptured;

    @PolyImmutable Thief(@UnderInitialization @PolyImmutable SuperClass2 victimCaptured) {
        this.victimCaptured = victimCaptured;
    }
}

public class SuperClass2{
    @PolyImmutable Date p;
    @NotOnlyInitialized @PolyImmutable
    Thief thief;

    @Mutable SuperClass2(@Mutable Date p){
        this.p = p;
        // "this" escapes constructor and gets captured by thief
        this.thief = new @Mutable Thief(this);
    }
}

class SubClass2 extends SuperClass2{
    @Immutable SubClass2(){
        // This is ok even though super constructor is @Mutable. Because all mutable objects are local within current constructor
        super(new @Mutable Date());
    }
}

class AnotherSubClass2 extends SuperClass2{
    @PolyImmutable AnotherSubClass2(){
        // This is ok even though super constructor is @Mutable. Because all mutable objects are local within current constructor
        super(new @Mutable Date());
    }
}
