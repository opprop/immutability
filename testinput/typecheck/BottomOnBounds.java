import java.util.List;
import qual.Bottom;

// :: error: (type.invalid)
public class BottomOnBounds<T extends @Bottom Object> {

    // PASS!
    void test1(List<? super @Bottom Object> l) {}

    // :: error: (type.invalid)
    void test2(List<? extends @Bottom Object> l) {}

    // :: error: (type.invalid)
    <S extends @Bottom Object> void test3(S s) {}
}
