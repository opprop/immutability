// https://github.com/opprop/checker-framework-inference/issues/112
class LocalVariableRefinement {
    void test1() {
        // TODO Should generate a RefinementVariableSlot for o
        // https://github.com/opprop/checker-framework-inference/issues/112
        Object o = new Object();
    }

    void test2() {
        Object o;
        // Already generating RefinementVariableSlot for o
        o = new Object();
    }
}
