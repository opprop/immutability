// @skip-test
// Guess: in glacter @MaybeMutable is super type of @Immutable. So the use of typevar is allowed.

import qual.Mutable;

public class ResultWrapTest {

    ResultWrapTest() {
        // while visiting this, the return type must be annotated correctly?
    }

    static class ResultWrap<T extends @Mutable Object> {
    }

    final ResultWrap<String> input = null;
}