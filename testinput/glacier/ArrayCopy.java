// @skip-test
// Typecheck: Failed
// Cause: checker cannot get clone() decl from stub! Decl receiver and return type get defaulted to mutable.
// strange enough, seems other signatures are got from stubs.
import qual.Immutable;


public class ArrayCopy {
    public void takeArray(@Immutable Object @Immutable [] array) {
    }

    public void passArray() {
        @Immutable Object array[] = new @Immutable Object[5];

        takeArray(array.clone());
    }

}