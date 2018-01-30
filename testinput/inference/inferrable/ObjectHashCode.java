import java.util.Objects;

enum Kind {
    SOME;
}
public class ObjectHashCode {
    Kind kind;

    @Override
    public int hashCode() {
        return Objects.hashCode(kind);
    }
}
