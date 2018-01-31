// class A is to simulate classes that are from bytecode, but this
// class still has its tree. But if we add the compiled class file
// to the classpath, the it should trigger the PicoImplicitTypeAnnotator
// change to apply @Immutable to enum type.
class A {

    public static enum Kind {
        SOME
    }
    public static void foo(Kind kind) {}
}

public class EnumParam {

    public static void main(String[] args) {
        A.foo(A.Kind.SOME);
    }
}
