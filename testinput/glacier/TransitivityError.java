// @skip-test
// until the defaulting of fields in immutable class is resolved

package glacier;

import qual.Immutable;


class TE_Inner {
    int x;
}

public @Immutable class TransitivityError {
    // :: error: glacier.mutable.invalid
    TE_Inner i;

    public TransitivityError() {
    }

    public void test() {

    }
}