package pico.typecheck;

import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

import java.util.Map;

/** Created by mier on 15/08/17. */
public class PICONoInitStore extends CFAbstractStore<PICONoInitValue, PICONoInitStore> {

    protected Map<FieldAccess, PICONoInitValue> initializedFields;

    public PICONoInitStore(
            CFAbstractAnalysis<PICONoInitValue, PICONoInitStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public PICONoInitStore(PICONoInitStore s) {
        super(s);
        if (s.initializedFields != null) {
            initializedFields = s.initializedFields;
        }
    }
}
