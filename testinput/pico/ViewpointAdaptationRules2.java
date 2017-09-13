import qual.Bottom;
import qual.Mutable;
import qual.Immutable;
import qual.PolyImmutable;
import qual.Readonly;

public class ViewpointAdaptationRules2 {

    @Bottom Object bf;

    @PolyImmutable ViewpointAdaptationRules2(@Bottom Object bf) {
        this.bf = bf;
    }
}