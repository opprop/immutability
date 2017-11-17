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
        checkInvalidBottom(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitDeclared(type, tree);
    }

    private void checkStaticReceiverDependantMutableError(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor, PICOInferenceChecker picoInferenceChecker) {
        if (isInStaticContext()) {
            if (picoInferenceVisitor.infer) {
                picoInferenceVisitor.mainIsNot(type, picoInferenceChecker.RECEIVERDEPENDANTMUTABLE, "static.receiverdependantmutable.forbidden", tree);
            } else {
                if (type.hasAnnotation(picoInferenceChecker.RECEIVERDEPENDANTMUTABLE)) {
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
                        new AnnotationMirror[]{picoInferenceChecker.READONLY, picoInferenceChecker.MUTABLE,
                                picoInferenceChecker.RECEIVERDEPENDANTMUTABLE, picoInferenceChecker.BOTTOM},
                        "type.invalid", tree);
            } else {
                if (!type.hasAnnotation(picoInferenceChecker.IMMUTABLE)) {
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
            picoInferenceVisitor.mainIsNot(type, picoInferenceChecker.BOTTOM, "type.invalid", tree);
        } else {
            if (type.hasAnnotation(picoInferenceChecker.BOTTOM)) {
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
        checkInvalidBottom(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitArray(type, tree);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        PICOInferenceChecker picoInferenceChecker = picoInferenceVisitor.realChecker;
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitPrimitive(type, tree);
    }

    @Override
    public Void visitNull(AnnotatedNullType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        PICOInferenceChecker picoInferenceChecker = picoInferenceVisitor.realChecker;
        if (picoInferenceVisitor.infer) {
            picoInferenceVisitor.mainIs(type, picoInferenceChecker.BOTTOM, "type.invalid", tree);
        } else {
            if (!type.hasAnnotation(picoInferenceChecker.BOTTOM)) {
                reportError(type, tree);
            }
        }
        return super.visitNull(type, tree);
    }

    /**Decides if the visitor is in static context right now*/
    private boolean isInStaticContext(){
        boolean isStatic = false;
        MethodTree meth = TreeUtils.enclosingMethod(visitor.getCurrentPath());
        if(meth != null){
            ExecutableElement methel = TreeUtils.elementFromDeclaration(meth);
            isStatic = ElementUtils.isStatic(methel);
        } else {
            BlockTree blcktree = TreeUtils.enclosingTopLevelBlock(visitor.getCurrentPath());
            if (blcktree != null) {
                isStatic = blcktree.isStatic();
            } else {
                VariableTree vartree = TreeUtils.enclosingVariable(visitor.getCurrentPath());
                if (vartree != null) {
                    ModifiersTree mt = vartree.getModifiers();
                    isStatic = mt.getFlags().contains(Modifier.STATIC);
                }
            }
        }
        return isStatic;
    }
}
