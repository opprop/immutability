import qual.*;

@ReceiverDependantMutable
class A {
    @Assignable B b;
    @ReceiverDependantMutable A() {}
    void bar(@Readonly A this) {}
}

class B {

}

class Super {
    void foo() {}
}

public class ObjectIdentityMethodTest extends Super{
    static A a0;
    /*Fields NOT in abstract states*/
    @Assignable A a1;
    @Mutable A a2;
    @Readonly A a3;
    /*Fields in abstract state*/
    A a4;// default is @RDA @RDM
    @Immutable A a5;
    final A a6;
    final @Immutable A a7;

    ObjectIdentityMethodTest() {
        a6 = new A();
        a7 = new @Immutable A();
    }

    @ObjectIdentityMethod
    public void testFieldAcess() {
        // :: error: (object.identity.static.field.access.forbidden)
        a0.bar();
        // :: error: (object.identity.field.access.invalid)
        a1.bar();
        // :: error: (object.identity.field.access.invalid)
        a2.bar();
        // :: error: (object.identity.field.access.invalid)
        a3.bar();
        // Don't transitively check bar is object identity method => shallow object identity check!
        a4.bar();
        // Don't transitively check b is in abstract state => shallow object identity check!
        Object o = a4.b;
        a5.bar();
        a6.bar();
        a7.bar();
        // :: error: (object.identity.field.access.invalid)
        this.a1.bar();
        // :: error: (object.identity.field.access.invalid)
        this.a2.bar();
        // :: error: (object.identity.field.access.invalid)
        this.a3.bar();
        this.a4.bar();
        // Similar argument for shallow object identity check as above
        Object o2 = this.a4.b;
        this.a5.bar();
        this.a6.bar();
        this.a7.bar();
    }

    @ObjectIdentityMethod
    void testMethodInvocation(ObjectIdentityMethodTest p, A a) {
        // :: error: (object.identity.method.invocation.invalid)
        foo();
        // :: error: (object.identity.method.invocation.invalid)
        this.foo();
        // :: error: (object.identity.method.invocation.invalid)
        super.foo();
        // Doesn't check the below two method invocation, as they are not invoked on the same receiver as
        // current receiver "this"
        p.foo();
        a.bar();
    }

    void foo(){}

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // :: error: (object.identity.field.access.invalid)
        result += a1.hashCode();
        // :: error: (object.identity.field.access.invalid)
        result += a2.hashCode();
        // :: error: (object.identity.field.access.invalid)
        result += a3.hashCode();
        result += a4.hashCode();
        result += a5.hashCode();
        result += a6.hashCode();
        result += a7.hashCode();
        return result;
    }
}
