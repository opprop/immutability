import qual.Immutable;
import qual.Mutable;

import java.util.ArrayList;
import java.util.List;

class A {
    A() {}
}

@Immutable
class B {
    @Immutable B() {}
}

public class DefaultRDMElementUsageToMutable {
    void foo() {
        // ArrayList didn't inherit with @ReceiverDependentMutable
        // and is defaulted to @Mutable
        @Mutable List<String> list = new ArrayList<>();
        // @Mutable type element gets inheritted @Mutable
        @Mutable A a = new A();
        // @Immutable type element gets inheritted @Immutable
        @Immutable B b = new B();
    }
}
