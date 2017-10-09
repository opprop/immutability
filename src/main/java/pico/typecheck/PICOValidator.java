package pico.typecheck;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import qual.Immutable;
import qual.ReceiverDependantMutable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Created by mier on 29/09/17.
 */
public class PICOValidator extends BaseTypeValidator {
    public PICOValidator(BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    private boolean isInStaticContext(){
        boolean isstatic = false;
        MethodTree meth = TreeUtils.enclosingMethod(visitor.getCurrentPath());
        if(meth != null){
            ExecutableElement methel = TreeUtils.elementFromDeclaration(meth);
            isstatic = ElementUtils.isStatic(methel);
        } else {
            BlockTree blcktree = TreeUtils.enclosingTopLevelBlock(visitor.getCurrentPath());
            if (blcktree != null) {
                isstatic = blcktree.isStatic();
            } else {
                VariableTree vartree = TreeUtils.enclosingVariable(visitor.getCurrentPath());
                if (vartree != null) {
                    ModifiersTree mt = vartree.getModifiers();
                    isstatic = mt.getFlags().contains(Modifier.STATIC);
                }
            }
        }
        return isstatic;
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        if (isInStaticContext() && type.hasAnnotation(ReceiverDependantMutable.class)) {
            // TODO Remove duplicate warnings
            checker.report(
                    Result.failure(
                            "static.receiverdependantmutable.forbidden", type), tree);
        }
        if (PICOTypeUtil.isBoxedPrimitiveOrString(type)) {
            checkPrimitiveBoxedPrimitiveStringTypeError(type, tree);
        }
        if (tree.getKind() == Kind.VARIABLE) {
            VariableTree variableTree = (VariableTree) tree;
            VariableElement variableElement = TreeUtils.elementFromDeclaration(variableTree);
            if (!checkOnlyOneAssignability(variableElement)) {
                checker.report(Result.failure("one.assignability.invalid", variableElement), variableElement);
            }
        }
        return super.visitDeclared(type, tree);
    }

    private boolean checkOnlyOneAssignability(VariableElement variableElement) {
        PICOAnnotatedTypeFactory picoAnnotatedTypeFactory = (PICOAnnotatedTypeFactory) atypeFactory;
        if (picoAnnotatedTypeFactory.isAssignableField(variableElement) && !picoAnnotatedTypeFactory.isFinalField(variableElement) &&
                !picoAnnotatedTypeFactory.isReceiverDependantAssignable(variableElement)) {
            return true;
        } else if (!picoAnnotatedTypeFactory.isAssignableField(variableElement) && picoAnnotatedTypeFactory.isFinalField(variableElement) &&
                !picoAnnotatedTypeFactory.isReceiverDependantAssignable(variableElement)) {
            return true;
        } else if (!picoAnnotatedTypeFactory.isAssignableField(variableElement) && !picoAnnotatedTypeFactory.isFinalField(variableElement) &&
                picoAnnotatedTypeFactory.isReceiverDependantAssignable(variableElement)) {
            return true;
        }
        return false;
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        checkPrimitiveBoxedPrimitiveStringTypeError(type, tree);
        return super.visitPrimitive(type, tree);
    }


    private void checkPrimitiveBoxedPrimitiveStringTypeError(AnnotatedTypeMirror type, Tree tree) {
        if (!type.hasAnnotation(Immutable.class)) {
            reportError(type, tree);
        }
    }
}
