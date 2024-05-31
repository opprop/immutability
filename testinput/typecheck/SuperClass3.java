package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

import java.util.Date;

@ReceiverDependentMutable
public class SuperClass3 {
    @ReceiverDependentMutable Date p;

    @ReceiverDependentMutable SuperClass3(@ReceiverDependentMutable Date p) {
        this.p = p;
    }
}

class SubClass3 extends SuperClass3 {
    @Mutable SubClass3(){
        super(new @Mutable Date(1L));
    }
}

@Immutable
class AnotherSubClass3 extends SuperClass3 {
    @Immutable AnotherSubClass3(){
        super(new @Immutable Date(1L));
    }
}

@ReceiverDependentMutable
class ThirdSubClass3 extends SuperClass3 {
    @ReceiverDependentMutable ThirdSubClass3(){
        super(new @ReceiverDependentMutable Date(1L));
    }
}
