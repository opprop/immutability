package pico.typecheck;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.javacutil.Pair;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

/**
 * Created by mier on 15/08/17.
 */
public class PICOAnalysis extends CFAbstractAnalysis<PICOValue, PICOStore, PICOTransfer> {

    public PICOAnalysis(BaseTypeChecker checker, PICOAnnotatedTypeFactory factory, List<Pair<VariableElement, PICOValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public PICOStore createEmptyStore(boolean sequentialSemantics) {
        return new PICOStore(this, sequentialSemantics);
    }

    @Override
    public PICOStore createCopiedStore(PICOStore picoStore) {
        return new PICOStore(this, picoStore);
    }

    @Override
    public PICOValue createAbstractValue(Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
        return new PICOValue(this, annotations, underlyingType);
    }
}
