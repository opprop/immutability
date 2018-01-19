import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

// https://github.com/opprop/checker-framework-inference/issues/130

class A {}

class Wrapper<T> {
    Wrapper(T t) {}
}
public class NullDeclSlot {
    public static void foo(A a) {
        //new Wrapper<A>(a);// This is ok
        new Wrapper<>(a);// This causes exception(in pico inference side, caused null decl slot when viewpoint adapt
        // constructor parameter type to usage type because there is no VarAnnot on constructor parameter type, in
        // which constructor is Wrapper<A>(A t))
    }
}
