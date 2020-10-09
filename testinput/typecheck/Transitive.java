import qual.Readonly;

public class Transitive {

    // class A, B, C are not annotated to test transitive mutability by default.

    static class A {
        B b;

        public B getB() {
            return b;
        }
    }

    static class B {
        int field = 0;
        C c;

        public C getC() {
            return c;
        }
    }

    static class C {
        int field = 0;
    }

    static class Caller {
        void test(@Readonly A a) {
            // error
            a.b.field = 1;
            // error
            a.getB().field = 1;

            // error
            a.b.c.field = 1;
            // error
            a.getB().getC().field = 1;
            // error
            a.b.getC().field = 1;
            // error
            a.getB().c.field = 1;
        }
    }
}

