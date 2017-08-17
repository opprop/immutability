package pico.typecheck;

import org.checkerframework.checker.initialization.InitializationTransfer;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/**
 * Created by mier on 15/08/17.
 */
public class PICOTransfer extends InitializationTransfer<PICOValue, PICOTransfer, PICOStore>{

    public PICOTransfer(PICOAnalysis analysis) {
        super(analysis);
    }
}
