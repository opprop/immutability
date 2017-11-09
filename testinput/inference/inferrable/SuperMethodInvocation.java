import qual.Mutable;

public class SuperMethodInvocation {

    Object f;

    void foo(SuperMethodInvocation this) {
        this.f = new @Mutable Object();
    }
}

class SubClass extends SuperMethodInvocation{

    @Override
    void foo(SubClass this) {
        super.foo();
    }
}
