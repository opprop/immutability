import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

@Mutable
public class CompatabilityBEI1 {}

@Immutable
class A {}

@ReceiverDependentMutable
class B {}

@Mutable
class C implements Cloneable {}

@Immutable
class D implements Cloneable {}

@ReceiverDependentMutable
class E implements Cloneable {}

@Mutable
class F extends Object {}

@Immutable
class G extends Object {}

@ReceiverDependentMutable
class H extends Object {}

@Mutable
class I implements @Mutable Cloneable {}

// :: error: (declaration.inconsistent.with.implements.clause)
@Mutable class J implements @Immutable Cloneable {}

@Mutable class K implements @ReceiverDependentMutable Cloneable {}

// :: error: (declaration.inconsistent.with.extends.clause)
@Immutable class L extends @Mutable Object {}

@Immutable class M extends @Immutable Object {}

@Immutable class N extends @ReceiverDependentMutable Object {}

abstract class O implements CharSequence {}

@Mutable abstract class P implements CharSequence {}

@Immutable abstract class Q implements CharSequence {}

@ReceiverDependentMutable abstract class R implements CharSequence {}
