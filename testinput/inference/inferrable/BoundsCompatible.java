import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

@ReceiverDependantMutable
public class BoundsCompatible {}

// @Mutable propagates here
class Level1A extends BoundsCompatible {}

// @Immutable propagates here
class Level1B extends BoundsCompatible {}

@Mutable class Level2A extends Level1A {}

// :: fixable-error: (subclass.bound.incompatible)
@Immutable class Level2B extends Level1B {}
