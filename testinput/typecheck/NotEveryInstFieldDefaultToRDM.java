import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

@ReceiverDependantMutable
public class NotEveryInstFieldDefaultToRDM {
    // :: error: (assignment.type.incompatible)
    @ReceiverDependantMutable B b1 = new B();
    B b2 = new @ReceiverDependantMutable B();
    C c = new @Mutable C();
    D d = new @Mutable D();
    E e = new @Immutable E();
}

@ReceiverDependantMutable
class B {}

class C {}

@Mutable
class D {}

@Immutable
class E {}
