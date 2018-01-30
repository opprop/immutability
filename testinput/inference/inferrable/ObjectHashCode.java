import java.util.Objects;

enum Kind {
    SOME;
}
public class ObjectHashCode {
    Kind kind;

    ObjectHashCode(Kind k) {
        kind = k;
        kind = Kind.SOME;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(kind);
    }
}
