package pico.inference;

import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WildcardTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import pico.typecheck.PICOTypeUtil;
import qual.Bottom;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

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
        PICOInferenceChecker picoInferenceChecker = picoInferenceVisitor.realChecker;
        checkStaticReceiverDependantMutableError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        //checkInvalidBottom(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitDeclared(type, tree);
    }

    private void checkStaticReceiverDependantMutableError(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor, PICOInferenceChecker picoInferenceChecker) {
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
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor, PICOInferenceChecker picoInferenceChecker) {
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

    private void checkInvalidBottom(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor, PICOInferenceChecker picoInferenceChecker) {
        if (tree instanceof WildcardTree) {
            AnnotatedWildcardType awt = (AnnotatedWildcardType) atypeFactory.getAnnotatedTypeFromTypeTree(tree);
            if (awt.getSuperBound().equals(type)) {
                // Means that we're checking the usage of @Bottom on the super(of wildcard).
                // But @Bottom can be used on lower bounds, so skip the check
                return;
            }
        }

        if (picoInferenceVisitor.infer) {
            picoInferenceVisitor.mainIsNot(type, BOTTOM, "type.invalid", tree);
        } else {
            if (type.hasAnnotation(BOTTOM)) {
                reportError(type, tree);
            }
        }
    }

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        PICOInferenceChecker picoInferenceChecker = picoInferenceVisitor.realChecker;
        checkStaticReceiverDependantMutableError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        //checkInvalidBottom(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitArray(type, tree);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        PICOInferenceChecker picoInferenceChecker = picoInferenceVisitor.realChecker;
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitPrimitive(type, tree);
    }
}
