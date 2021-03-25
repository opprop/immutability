// Note: PICO defaults the class decal to RDM. Removing errors on line 26 and 30.

import qual.*;

@Immutable  interface SColor {
}

@Immutable abstract class AbstractColorAdv implements SColor {
}

// ::error: (type.invalid.annotations.on.use)
class FI_ColorImpl extends AbstractColorAdv {
    // Arguably it would be preferable for this to not be an error.
    // ::error: (type.invalid.annotations.on.use)
    public static final AbstractColorAdv BLACK = new FI_ColorImpl("#000000");

    // PICO Note: adding this error.
    // ::error: (super.invocation.invalid)
    FI_ColorImpl(String fontColor) {

    }
}

public class FontImpl {
    FontImpl(String fontColor) {
        // Arguably it would be preferable for this to not be an error.
        // Removing error. PICO chose the preferable path.
        SColor a = new FI_ColorImpl(fontColor);

        // Arguably it would be preferable for this to not be an error either.
        // Removing error. PICO chose the preferable path.
        SColor c = fontColor != null ? new FI_ColorImpl(fontColor) : null;
    }
}