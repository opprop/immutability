class NeedPreAnnotatedStub {
    void bar(A a) {
        System.out.print(a.foo());
    }
}
class A {
    Object foo() {
        return null;
    }
}
