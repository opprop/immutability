import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;

import java.util.Date;

public class SuperClass{
    @PolyImmutable Date p;
    @Immutable SuperClass(@Immutable Date p){
        this.p = p;
    }

    void maliciouslyModifyDate(@Mutable SuperClass this){
        p.setTime(2L);
    }
}

class SubClass extends SuperClass{
    @Mutable SubClass(){
        //:: error: (constructor.invocation.invalid)
        super(new @Immutable Date(1L));
    }

    public static void main(String[] args) {
        @Mutable SubClass victim = new @Mutable SubClass();
        victim.maliciouslyModifyDate();;
    }
}

class AnotherSubClass extends SuperClass{
    @PolyImmutable AnotherSubClass(){
        //:: error: (constructor.invocation.invalid)
        super(new @Immutable Date(1L));
    }

    public static void main(String[] args) {
        @Mutable SubClass victim = new @Mutable SubClass();
        victim.maliciouslyModifyDate();;
    }
}
