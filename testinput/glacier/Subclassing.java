// @skip-test
// Drop this one: PICO cannot derive from mutable classes

import qual.Immutable;

public class Subclassing { };

class MutSubclass extends Subclassing { };

// OK with the recent change: immutable classes can derive from mutable classes
@Immutable class InvalidImmutSubclass extends Subclassing { };


@Immutable class ImmutableSuper { };
@Immutable class ImmutableSub extends ImmutableSuper { };

// ::error: (glacier.subclass.mutable)
class InvalidMutableSub extends ImmutableSuper { };