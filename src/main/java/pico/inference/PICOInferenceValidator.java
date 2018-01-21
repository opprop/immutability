package pico.inference;

import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.TreeUtils;
import pico.typecheck.PICOTypeUtil;

import javax.lang.model.element.AnnotationMirror;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

/**
 * Generates constraints based on PICO constraint-based well-formedness rules in infer mode.
 * In typecheck mode, it behaves exactly like PICOValidator
 */
public class PICOInferenceValidator extends InferenceValidator{
    public PICOInferenceValidator(BaseTypeChecker checker, InferenceVisitor<?, ?> visitor, AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        checkStaticReceiverDependantMutableError(type, tree, picoInferenceVisitor);
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor);
        return super.visitDeclared(type, tree);
    }


    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        checkStaticReceiverDependantMutableError(type, tree, picoInferenceVisitor);
        return super.visitArray(type, tree);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor);
        return super.visitPrimitive(type, tree);
    }

    private void checkStaticReceiverDependantMutableError(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor) {
        if (TreeUtils.isTreeInStaticScope(visitor.getCurrentPath())) {
            if (picoInferenceVisitor.infer) {
                picoInferenceVisitor.mainIsNot(type, RECEIVER_DEPENDANT_MUTABLE, "static.receiverdependantmutable.forbidden", tree);
            } else {
                if (type.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                    reportValidityResult("static.receiverdependantmutable.forbidden", type, tree);
                }
            }
        }
    }

    /**Check that implicitly immutable type has immutable annotation. Note that bottom will be handled uniformly on all
     the other remaining types(reference or primitive), so we don't handle it again here*/
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor) {
        if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
            if (picoInferenceVisitor.infer) {
                picoInferenceVisitor.mainIsNoneOf(type,
                        new AnnotationMirror[]{READONLY, MUTABLE, RECEIVER_DEPENDANT_MUTABLE, BOTTOM},
                        "type.invalid", tree);
            } else {
                if (!type.hasAnnotation(IMMUTABLE)) {
                    reportError(type, tree);
                }
            }
        }
    }
}
