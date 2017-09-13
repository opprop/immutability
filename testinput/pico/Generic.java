import org.checkerframework.checker.initialization.qual.Initialized;
import qual.Readonly;
import qual.Immutable;
import qual.Mutable;

import java.util.Date;

class Wrapper<T>{
    T t;
    @Immutable Wrapper(T t) {
        this.t = t;
    }
}

public class Generic {
    void test() {
        @Mutable Object arg = new @Mutable Object();
        @Initialized @Immutable Wrapper<@Mutable Object> wrapper = new @Immutable Wrapper<@Mutable Object>(arg);
        @Mutable Object mo = wrapper.t;
        //@Immutable List<@Mutable Object> l = new @Immutable<>();
    }

    void maliciouslyInjectMutableObject() {
        @Mutable Date md = new @Mutable Date();
        @Readonly Date spy = md;
        @Initialized @Immutable Wrapper<@Readonly Date> victim = new @Immutable Wrapper<@Readonly Date>(spy);
        // Now spy is modified and immutability guarantee is broken
        md.setTime(123L);
    }
}
