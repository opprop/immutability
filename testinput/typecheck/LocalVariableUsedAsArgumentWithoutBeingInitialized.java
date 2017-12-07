class A {
    A(B b) {}
}

class B {

}

public class LocalVariableUsedAsArgumentWithoutBeingInitialized {

    private A a;

    private A foo() {
        // Default type for local variable b is @UnknownInitialization @Readonly
        B b = null;

        // Simplified version of testcase that has the same effect as the while loop below.
        if (true) {
            // Statically this is not guaranteed to be executed
            b = new B();
        }

        // Similar structure in exp4j#Tokenizer
        /*while (true) {
            B op = getB();
            if (op == null) {
                op = new B();
                break;
            }else{
                b = op;
                break;
            }
        }*/

        // So b is never properly refined to a concrete type, so it stays as the declared type
        // Issues "argument.type.incompatible" because @UnknownInitialization @Readonly B is not
        // subtype of @Initialized @Mutable B
        // Issues "initialization.invalid.field.write.initialized" because new A(b) returns
        // @UnderInitialization @Mutable A, and @UnderInitialization argument is not allowed to
        // be written into @Initialized field a.
        // :: error: (argument.type.incompatible) :: error: (initialization.invalid.field.write.initialized)
        a = new A(b);
        return a;
    }

    // private B getB() {return null;}
}
