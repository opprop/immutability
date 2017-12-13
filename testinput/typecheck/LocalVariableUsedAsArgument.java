class A {
    A(B b) {}
}

class B {

}

public class LocalVariableUsedAsArgument {

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

        a = new A(b);
        return a;
    }

    // private B getB() {return null;}
}
