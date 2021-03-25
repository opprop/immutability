import qual.Immutable;

@SuppressWarnings("initialization")
@Immutable class ImmutableArray {
    private byte @Immutable [] _rgb;

    private String @Immutable [] _strings;

    // Immutable array of mutable objects is mutable.
    // PICO allows shallow immutable array.
    private java.util.Date @Immutable [] _dates;

    // MaybeMutable array of primitives is mutable.
    // PICO allows shallow immutable array.
    private int [] _ints;
}