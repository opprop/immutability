// @skip-test
// PICO CAN handle null to @Bottom

import qual.*;

class PlainObjects {

    public void takeObject(Object o) {};
    public void takeImmutableObject(@Immutable Object o) {};

    void foo () {
        Object o1 = null;
        @Immutable Object o2 = null;

        takeObject(o2);

        takeImmutableObject(o1);
    }
}