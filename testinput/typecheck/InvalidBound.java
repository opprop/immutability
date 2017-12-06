import qual.Bottom;
import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

// :: error: (class.bound.invalid)
@Readonly public class InvalidBound {}

// :: error: (class.bound.invalid)
@PolyMutable class A{}

// :: error: (class.bound.invalid)
@Bottom class B{}

// ok
@Immutable class C{ @Immutable C(){}}

// ok
@Mutable class D{}

// ok
@ReceiverDependantMutable class E{}
