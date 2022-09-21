import qual.Immutable;

@SuppressWarnings("initialization")
@Immutable
public class ImmutablePrimitiveContainer {
    int x;

    public void setX(int x) {
        // ::error: (illegal.field.write)
        this.x = x;
    }
}