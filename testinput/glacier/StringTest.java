package glacier;

import qual.Immutable;

import java.lang.String;

@SuppressWarnings("initialization")
public @Immutable class StringTest {
    String s; // no error expected here because String should be treated as if it were declared @Immutable.
}