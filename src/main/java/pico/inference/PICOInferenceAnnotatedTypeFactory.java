package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstraintManager;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

/**
 * Created by mier on 13/08/17.
 */
public class PICOInferenceAnnotatedTypeFactory extends InferenceAnnotatedTypeFactory {
    public PICOInferenceAnnotatedTypeFactory(InferenceChecker inferenceChecker, boolean withCombineConstraints, BaseAnnotatedTypeFactory realTypeFactory, InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        super(inferenceChecker, withCombineConstraints, realTypeFactory, realChecker, slotManager, constraintManager);
        postInit();
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new PICOInferenceTypeHierarchy(checker, getQualifierHierarchy(), varAnnot);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new ImplicitsTreeAnnotator(this), new PICOInferenceTreeAnnotater(this,
                realChecker, realTypeFactory, variableAnnotator, slotManager));
    }

    @Override
    protected VariableAnnotator createVariableAnnotator(BaseAnnotatedTypeFactory realTypeFactory, InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        return new PICOVariableAnnotator(this, realTypeFactory, realChecker, slotManager, constraintManager);
    }
}
