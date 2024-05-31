import qual.Bottom;
import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;
import qual.ReceiverDependentMutable;

// :: error: (class.bound.invalid)
@Readonly public class InvalidBound {}

// :: error: (class.bound.invalid)
@PolyMutable class A{}

// ok
@Immutable class C{ @Immutable C(){}}

// ok
@Mutable class D{}

// ok
@ReceiverDependentMutable class E{}
