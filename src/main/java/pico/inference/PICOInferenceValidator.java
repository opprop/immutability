package pico.inference;

import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import pico.typecheck.PICOTypeUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 * Generates constraints based on PICO constraint-based well-formedness rules in infer mode.
 * In typecheck mode, it behaves exactly like PICOValidator
 * TODO Add logic to enforece type rules are not violated in typecheck mode
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
        // TODO Not all declared types are forbidden to use @Bottom, for example, lower bounds
        checkBottom(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitDeclared(type, tree);
    }

    private void checkStaticReceiverDependantMutableError(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor, PICOInferenceChecker picoInferenceChecker) {
        if (picoInferenceVisitor.infer && isInStaticContext()) {
            picoInferenceVisitor.mainIsNot(type, picoInferenceChecker.RECEIVERDEPENDANTMUTABLE, "static.receiverdependantmutable.forbidden", tree);
        }
    }

    /**Check that implicitly immutable type has immutable annotation. Note that bottom will be handled uniformly on all
     the other remaining types(reference or primitive), so we don't handle it again here*/
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor, PICOInferenceChecker picoInferenceChecker) {
        if (picoInferenceVisitor.infer && PICOTypeUtil.isImplicitlyImmutableType(type)) {
            picoInferenceVisitor.mainIsNoneOf(type,
                    new AnnotationMirror[]{picoInferenceChecker.READONLY, picoInferenceChecker.MUTABLE, picoInferenceChecker.RECEIVERDEPENDANTMUTABLE},
                    "type.invalid", tree);
        }
    }

    private void checkBottom(AnnotatedTypeMirror type, Tree tree, PICOInferenceVisitor picoInferenceVisitor, PICOInferenceChecker picoInferenceChecker) {
        if (picoInferenceVisitor.infer) {
            picoInferenceVisitor.mainIsNot(type, picoInferenceChecker.BOTTOM, "type.invalid", tree);
        }
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        PICOInferenceChecker picoInferenceChecker = picoInferenceVisitor.realChecker;
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitPrimitive(type, tree);
    }

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        PICOInferenceVisitor picoInferenceVisitor = (PICOInferenceVisitor) visitor;
        PICOInferenceChecker picoInferenceChecker = picoInferenceVisitor.realChecker;
        checkStaticReceiverDependantMutableError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        checkImplicitlyImmutableTypeError(type, tree, picoInferenceVisitor, picoInferenceChecker);
        // TODO Not all declared types are forbidden to use @Bottom, for example, lower bounds
        checkBottom(type, tree, picoInferenceVisitor, picoInferenceChecker);
        return super.visitArray(type, tree);
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
