package typecheck;

import qual.Readonly;

public class ReadonlyConstructor {

    // :: error: (constructor.return.invalid)
    @Readonly ReadonlyConstructor() {}
}
