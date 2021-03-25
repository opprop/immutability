// @skip-test
// who cares bottom

import qual.*;

// ::error: (type.invalid)
@Bottom class InvalidBottom {};

public class InvalidAnnotations {
    // ::error: (type.invalid)
    InvalidBottom b;
}