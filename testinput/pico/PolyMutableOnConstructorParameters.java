import qual.Immutable;
import qual.PolyMutable;

public class PolyMutableOnConstructorParameters<T extends @PolyMutable Object> {
    @Immutable PolyMutableOnConstructorParameters(@PolyMutable Object o) {
    }

    public static void main(String[] args) {
        @Immutable PolyMutableOnConstructorParameters o1 = new @Immutable PolyMutableOnConstructorParameters(new @Immutable Object());
    }
}
