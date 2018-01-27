import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class MixedLubVarSlotWithVPAdaptation {

    Set<String> variableNames;

    private Object parseFunctionOrVariable() {
        // @Bottom is already viewpoint adapted to actual receiver type "variableNames"
        // (lower bound of type parameter of contains() method), but the later least upper
        // bound reused the same "CombVariableSlot" and generates subtype constraint:
        // typeof(variableNames) <: least upper bound, which is wrongly viewpoint adapted
        // result: @Bottom, thus caused variableNames to be @Bottom, and caused inference
        // to fail to give solutions. Fixed by adding LubVariableSlot in CFI.
        if (variableNames != null && variableNames.contains("")) {}
        return null;
    }
}
