package pico.inference;

import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.TreeUtils;
import pico.common.PICOTypeUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import java.util.Set;

import static pico.typecheck.PICOAnnotationMirrorHolder.*;

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
        checkStaticReceiverDependantMutableError(type, tree);
        checkImplicitlyImmutableTypeError(type, tree);
        checkOnlyOneAssignabilityModifierOnField(tree);
//        AnnotatedDeclaredType defaultType =
//                (AnnotatedDeclaredType) atypeFactory.getAnnotatedType(type.getUnderlyingType().asElement());
//        // TODO for defaulted super clause: should top anno be checked? (see shouldCheckTopLevelDeclaredOrPrimitiveType())
//        if (defaultType.getAnnotationInHierarchy(READONLY) == null && !PICOTypeUtil.isEnumOrEnumConstant(defaultType)) {
//            defaultType = defaultType.deepCopy();
//            defaultType.replaceAnnotation(MUTABLE);
//        }
//
//        if (!visitor.isValidUse(defaultType, type, tree)) {
//            reportInvalidAnnotationsOnUse(type, tree);
//        }
        // main != READONLY -> main |> bound <: main
        ((PICOInferenceVisitor) visitor).mainCannotInferTo(type, POLY_MUTABLE, "cannot.infer.poly", tree);
        return super.visitDeclared(type, tree);
    }

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        checkStaticReceiverDependantMutableError(type, tree);
        return super.visitArray(type, tree);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        checkImplicitlyImmutableTypeError(type, tree);
        return super.visitPrimitive(type, tree);
    }

    @Override
    protected boolean shouldCheckTopLevelDeclaredOrPrimitiveType(AnnotatedTypeMirror type, Tree tree) {
        if (TreeUtils.isLocalVariable(tree)) {
            return true;
        }
        return super.shouldCheckTopLevelDeclaredOrPrimitiveType(type, tree);
    }

    private void checkStaticReceiverDependantMutableError(AnnotatedTypeMirror type, Tree tree) {
        // Static inner class is considered within the static scope.
        // Added condition to ensure not class decl.
        if (PICOTypeUtil.inStaticScope(visitor.getCurrentPath()) && !type.isDeclaration()) {
//            if (infer) {
//                ((PICOInferenceVisitor)visitor).mainIsNot(type, RECEIVER_DEPENDANT_MUTABLE, "static.receiverdependantmutable.forbidden", tree);
//            } else {
//                if (type.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
//                    reportValidityResult("static.receiverdependantmutable.forbidden", type, tree);
//                }
//            }
            ((InferenceVisitor)visitor).mainIsNot(type, RECEIVER_DEPENDANT_MUTABLE, "static.receiverdependantmutable.forbidden", tree);
            // TODO set isValid or move to visitor
        }
    }

    /**Check that implicitly immutable type has immutable annotation. Note that bottom will be handled uniformly on all
     the other remaining types(reference or primitive), so we don't handle it again here*/
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree) {
        if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
            if (infer) {
                ((PICOInferenceVisitor)visitor).mainIsNoneOf(type,
                        new AnnotationMirror[]{READONLY, MUTABLE, RECEIVER_DEPENDANT_MUTABLE, BOTTOM},
                        "type.invalid.annotations.on.use", tree);
            } else {
                // FIXME workaround for typecheck. How should inference handle BOTTOM?
                if (!type.hasAnnotation(IMMUTABLE) && !type.hasAnnotation(BOTTOM)) {
                    reportInvalidAnnotationsOnUse(type, tree);
                }
            }
        }
    }

    /**Ensures the well-formdness in terms of assignability on a field. This covers both instance fields and static fields.*/
    private void checkOnlyOneAssignabilityModifierOnField(Tree tree) {
        if (tree.getKind() == Tree.Kind.VARIABLE) {
            VariableTree variableTree = (VariableTree) tree;
            VariableElement variableElement = TreeUtils.elementFromDeclaration(variableTree);
            if (infer) {
                // Do nothing in terms of assignability quaifier(no constraints generated), as we don't
                // support inferring assignability qualifier right now.
            } else {
                if (!PICOTypeUtil.hasOneAndOnlyOneAssignabilityQualifier(variableElement, atypeFactory)) {
                    reportFieldMultipleAssignabilityModifiersError(variableElement);
                }
            }
        }
    }

    private void reportFieldMultipleAssignabilityModifiersError(VariableElement field) {
        checker.reportError(field, "one.assignability.invalid", field);
        isValid = false;
    }

    private void checkLocalVariableDefaults(AnnotatedDeclaredType type, Tree tree) {
        Set<AnnotationMirror> bounds =
                atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());

        AnnotatedDeclaredType elemType = type.deepCopy();
        elemType.clearAnnotations();
        elemType.addAnnotations(bounds);

        if (!visitor.isValidUse(elemType, type, tree)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }
    }
}
