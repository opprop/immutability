import qual.Immutable;
import qual.Readonly;

public class Arrays{

    void test1(String @Immutable [] array) {
        //:: error: (illegal.array.write)
        array[0] = "something";
    }

    void test2() {
        //:: error: (pico.new.invalid)
        int [] a = new int @Readonly []{1,2};
    }

    void test3(String[] array) {
        array[0] = "something";
    }
}
