import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

import java.util.ArrayList;
import java.util.List;

@ReceiverDependentMutable
public class A extends ArrayList<@Immutable String> {}

class B extends A {}

// :: fixable-error: (type.invalid.annotations.on.use)
abstract class C implements List<@Immutable B> { }

interface II {}

class D implements II {}
