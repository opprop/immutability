package pico.typecheck;

import org.checkerframework.checker.initialization.InitializationStore;
import org.checkerframework.framework.flow.CFAbstractAnalysis;

/**
 * Created by mier on 15/08/17.
 */
public class PICOStore extends InitializationStore<PICOValue, PICOStore>{

    public PICOStore(CFAbstractAnalysis<PICOValue, PICOStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public PICOStore(CFAbstractAnalysis<PICOValue, PICOStore, ?> analysis, PICOStore other) {
        super(other);
    }
}
