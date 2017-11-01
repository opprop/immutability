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
import qual.Bottom;
import qual.Immutable;
import qual.ReceiverDependantMutable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Created by mier on 29/09/17.
 * Enforce correct usage of immutability and assignability qualifiers.
 * TODO Enforce @Bottom is used nowhere; @PolyMutable is only used on constructor/method parameters or method return
 */
public class PICOValidator extends BaseTypeValidator {
    public PICOValidator(BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        checkStaticReceiverDependantMutableError(type, tree);
        checkImplicitlyImmutableTypeError(type, tree);
        checkOnlyOneAssignabilityModifierOnField(tree);
        return super.visitDeclared(type, tree);
    }

    private void checkStaticReceiverDependantMutableError(AnnotatedDeclaredType type, Tree tree) {
        if (isInStaticContext() && type.hasAnnotation(ReceiverDependantMutable.class)) {
            reportValidityResult("static.receiverdependantmutable.forbidden", type, tree);
        }
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

    /**Check that implicitly immutable type has immutable annotation. Note that bottom will be handled uniformly on all
       the other remaining types(reference or primitive), so we don't handle it again here*/
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree) {
        if (PICOTypeUtil.isImplicitlyImmutableType(type) && !type.hasAnnotation(Immutable.class) && !type.hasAnnotation(Bottom.class)) {
            reportError(type, tree);
        }
    }

    /**Ensures the well-formdness in terms of assignability on a field. This covers both instance fields and static fields.*/
    private void checkOnlyOneAssignabilityModifierOnField(Tree tree) {
        if (tree.getKind() == Kind.VARIABLE) {
            VariableTree variableTree = (VariableTree) tree;
            VariableElement variableElement = TreeUtils.elementFromDeclaration(variableTree);
            boolean isValid = false;
            PICOAnnotatedTypeFactory picoAnnotatedTypeFactory = (PICOAnnotatedTypeFactory) atypeFactory;

            if (picoAnnotatedTypeFactory.isAssignableField(variableElement) && !picoAnnotatedTypeFactory.isFinalField(variableElement) &&
                    !picoAnnotatedTypeFactory.isReceiverDependantAssignable(variableElement)) {
                isValid = true;
            } else if (!picoAnnotatedTypeFactory.isAssignableField(variableElement) && picoAnnotatedTypeFactory.isFinalField(variableElement) &&
                    !picoAnnotatedTypeFactory.isReceiverDependantAssignable(variableElement)) {
                isValid = true;
            } else if (!picoAnnotatedTypeFactory.isAssignableField(variableElement) && !picoAnnotatedTypeFactory.isFinalField(variableElement) &&
                    picoAnnotatedTypeFactory.isReceiverDependantAssignable(variableElement)) {
                assert !ElementUtils.isStatic(variableElement);
                isValid = true;
            }

            if (!isValid) {
                reportFieldMultipleAssignabilityModifiersError(variableElement);
            }
        }
    }

    private void reportFieldMultipleAssignabilityModifiersError(VariableElement field) {
        checker.report(Result.failure("one.assignability.invalid", field), field);
        isValid = false;
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        checkImplicitlyImmutableTypeError(type, tree);
        return super.visitPrimitive(type, tree);
    }
}
