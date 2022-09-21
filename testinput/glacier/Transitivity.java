package glacier;

import qual.Immutable;

@SuppressWarnings("initialization")
@Immutable class Inner {
    int x;
}

@SuppressWarnings("initialization")
public @Immutable class Transitivity {
    Inner i;

    public Transitivity() {
    }

    public void test() {

    }
}