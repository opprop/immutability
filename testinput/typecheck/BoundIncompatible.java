import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;
public class BoundIncompatible implements java.io.Serializable{}

@Mutable
class A implements java.io.Serializable{}

@ReceiverDependantMutable
class B implements java.io.Serializable{}

@Immutable
class C implements java.io.Serializable{}
