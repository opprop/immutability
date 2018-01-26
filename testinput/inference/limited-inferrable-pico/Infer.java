
public class Infer<T extends Object> {
    T o;

    // https://github.com/opprop/checker-framework-inference/pull/144
    Infer(T o) {
        this.o = o;
    }
}


class Test extends Infer<Object>{

    Test(Object o) {
        super(o);
    }

    static void foo(Test t) {
        // TODO Here is the problem. Inference result causes overriding type on "T o" to be ignored
        // and still annotation on type argument Object is used as type of "t.o"
        Object l = t.o;
    }
}
