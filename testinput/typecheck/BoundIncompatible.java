import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

public class BoundIncompatible implements java.io.Serializable{}

@Mutable
class A implements java.io.Serializable{}

@ReceiverDependentMutable
class B implements java.io.Serializable{}

@Immutable
class C implements java.io.Serializable{}
