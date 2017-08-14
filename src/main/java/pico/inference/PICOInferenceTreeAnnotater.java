package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceTreeAnnotator;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

/**
 * Created by mier on 13/08/17.
 */
public class PICOInferenceTreeAnnotater extends InferenceTreeAnnotator {
    public PICOInferenceTreeAnnotater(InferenceAnnotatedTypeFactory atypeFactory, InferrableChecker realChecker, AnnotatedTypeFactory realAnnotatedTypeFactory, VariableAnnotator variableAnnotator, SlotManager slotManager) {
        super(atypeFactory, realChecker, realAnnotatedTypeFactory, variableAnnotator, slotManager);
    }
}
