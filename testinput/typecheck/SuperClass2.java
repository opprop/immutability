package typecheck;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

import java.util.Date;

@ReceiverDependentMutable
class Thief {
    @NotOnlyInitialized @ReceiverDependentMutable
    SuperClass2 victimCaptured;

    @ReceiverDependentMutable Thief(@UnderInitialization @ReceiverDependentMutable SuperClass2 victimCaptured) {
        this.victimCaptured = victimCaptured;
    }
}

@ReceiverDependentMutable
public class SuperClass2{
    @ReceiverDependentMutable Date p;
    @NotOnlyInitialized @ReceiverDependentMutable
    Thief thief;

    @Mutable SuperClass2(@Mutable Date p){
        this.p = p;
        // "this" escapes constructor and gets captured by thief
        this.thief = new @Mutable Thief(this);
    }
}

@Immutable
class SubClass2 extends SuperClass2{
    @Immutable SubClass2(){
        // This is not ok any more
        // :: error: (super.invocation.invalid)
        super(new @Mutable Date());
    }
}

@ReceiverDependentMutable
class AnotherSubClass2 extends SuperClass2{
    @ReceiverDependentMutable AnotherSubClass2(){
        // This is not ok any more
        // :: error: (super.invocation.invalid)
        super(new @Mutable Date());
    }
}
