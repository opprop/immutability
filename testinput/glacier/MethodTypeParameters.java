import qual.Immutable;

class TypeParameters<E extends @Immutable Object> {
    // :: error: (type.argument.type.incompatible)
    static <E> TypeParameters<E> asImmutableList(@Immutable Object[] elements) {
        return null;
    }

    // no error here
    static <V extends @Immutable Object> TypeParameters<V> asImmutableList2(@Immutable Object[] elements) {
        return null;
    }
}