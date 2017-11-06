import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

import java.util.Date;

public class SuperClass3 {
    @ReceiverDependantMutable Date p;

    @ReceiverDependantMutable SuperClass3(@ReceiverDependantMutable Date p) {
        this.p = p;
    }
}

class SubClass3 extends SuperClass3 {
    @Mutable SubClass3(){
        super(new @Mutable Date(1L));
    }
}

class AnotherSubClass3 extends SuperClass3 {
    @Immutable AnotherSubClass3(){
        super(new @Immutable Date(1L));
    }
}

class ThirdSubClass3 extends SuperClass3 {
    @ReceiverDependantMutable ThirdSubClass3(){
        super(new @ReceiverDependantMutable Date(1L));
    }
}
