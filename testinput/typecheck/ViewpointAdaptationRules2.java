package typecheck;

import qual.Bottom;

public class ViewpointAdaptationRules2 {

    // :: error: (type.invalid)
    @Bottom Object bf;

    // :: error: (type.invalid)
    ViewpointAdaptationRules2(@Bottom Object bf) {
        // :: error: (type.invalid)
        this.bf = bf;
    }
}
