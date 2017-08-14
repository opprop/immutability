package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstraintManager;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

/**
 * Created by mier on 13/08/17.
 */
public class PICOVariableAnnotator extends VariableAnnotator{
    public PICOVariableAnnotator(InferenceAnnotatedTypeFactory typeFactory, AnnotatedTypeFactory realTypeFactory, InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        super(typeFactory, realTypeFactory, realChecker, slotManager, constraintManager);
    }
}
