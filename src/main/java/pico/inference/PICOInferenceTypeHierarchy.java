package pico.inference;

import checkers.inference.InferenceTypeHierarchy;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;

import javax.lang.model.element.AnnotationMirror;

/**
 * Created by mier on 13/08/17.
 */
public class PICOInferenceTypeHierarchy extends InferenceTypeHierarchy {
    /**
     * Constructs an instance of {@code TypeHierarchy} for the type system
     * whose qualifiers represented in qualifierHierarchy.
     *
     * @param checker            The type-checker to use
     * @param qualifierHierarchy The qualifier hierarchy to use
     * @param varAnnot
     */
    public PICOInferenceTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy, AnnotationMirror varAnnot) {
        super(checker, qualifierHierarchy, varAnnot);
    }
}
