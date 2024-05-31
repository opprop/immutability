package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

import java.util.Date;

@ReceiverDependentMutable
public class SuperClass{
    @ReceiverDependentMutable Date p;
    @Immutable SuperClass(@Immutable Date p){
        this.p = p;
    }

    void maliciouslyModifyDate(@Mutable SuperClass this){
        p.setTime(2L);
    }
}

class SubClass extends SuperClass{
    @Mutable SubClass(){
        // :: error: (super.invocation.invalid)
        super(new @Immutable Date(1L));
    }

    public static void main(String[] args) {
        @Mutable SubClass victim = new @Mutable SubClass();
        victim.maliciouslyModifyDate();
    }
}

@ReceiverDependentMutable
class AnotherSubClass extends SuperClass{
    @ReceiverDependentMutable AnotherSubClass(){
        // :: error: (super.invocation.invalid)
        super(new @Immutable Date(1L));
    }

    public static void main(String[] args) {
        @Mutable SubClass victim = new @Mutable SubClass();
        victim.maliciouslyModifyDate();
    }
}
