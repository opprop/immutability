import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

@ReceiverDependentMutable
public class NotEveryInstFieldDefaultToRDM {
    // :: error: (assignment.type.incompatible)
    @ReceiverDependentMutable B b1 = new B();
    B b2 = new @ReceiverDependentMutable B();
    @Mutable C c = new @Mutable C();
    @Mutable D d = new @Mutable D();
    E e = new @Immutable E();
}

@ReceiverDependentMutable
class B {}

class C {}

@Mutable
class D {}

@Immutable
class E {}
