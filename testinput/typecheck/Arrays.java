package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.Readonly;

public class Arrays{

    void test1(String @Immutable [] array) {
        // :: error: (illegal.array.write)
        array[0] = "something";
    }

    void test2() {
        // :: error: (pico.new.invalid)
        int [] a = new int @Readonly []{1,2};
    }

    void test3(String[] array) {
        array[0] = "something";
    }

    /**This method is based on the assumption: Object class is implicitly @Readonly*/
    void test4(@Immutable String @Mutable [] p) {
        // :: error: (assignment.type.incompatible)
        Object [] l = p;// By default, array type is @Readonly(local variable); Object class is by default @Mutable. So assignment should not typecheck
    }

    void test5(@Immutable Integer @Mutable [] p) {
        // :: error: (assignment.type.incompatible)
        @Mutable Object @Readonly [] l = p;
    }
}
