package pico.typecheck;

import com.sun.tools.javac.code.Symbol;
import org.checkerframework.checker.initialization.InitializationTransfer;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

/**
 * Created by mier on 15/08/17.
 */
public class PICOTransfer extends InitializationTransfer<PICOValue, PICOTransfer, PICOStore>{

    public PICOTransfer(PICOAnalysis analysis) {
        super(analysis);
    }
}
