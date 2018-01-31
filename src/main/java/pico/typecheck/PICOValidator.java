package pico.typecheck;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import javax.lang.model.element.VariableElement;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

/**
 * Created by mier on 29/09/17.
 * Enforce correct usage of immutability and assignability qualifiers.
 * TODO @PolyMutable is only used on constructor/method parameters or method return
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

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        checkStaticReceiverDependantMutableError(type, tree);
        // Array can not be implicitly immutable
        return super.visitArray(type, tree);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        checkImplicitlyImmutableTypeError(type, tree);
        return super.visitPrimitive(type, tree);
    }

    private void checkStaticReceiverDependantMutableError(AnnotatedTypeMirror type, Tree tree) {
        if (PICOTypeUtil.inStaticScope(visitor.getCurrentPath())
                && !"".contentEquals(TreeUtils.enclosingClass(visitor.getCurrentPath()).getSimpleName())// Exclude @RDM usages in anonymous classes
                && type.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
            reportValidityResult("static.receiverdependantmutable.forbidden", type, tree);
        }
    }

    /**Check that implicitly immutable type has immutable or bottom type. Dataflow might refine immtable type to @Bottom,
     * so we accept @Bottom as a valid qualifier for implicitly immutable types*/
    private void checkImplicitlyImmutableTypeError(AnnotatedTypeMirror type, Tree tree) {
        if (PICOTypeUtil.isImplicitlyImmutableType(type) && !type.hasAnnotation(IMMUTABLE) && !type.hasAnnotation(BOTTOM)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }
    }

    /**Ensures the well-formdness in terms of assignability on a field. This covers both instance fields and static fields.*/
    private void checkOnlyOneAssignabilityModifierOnField(Tree tree) {
        if (tree.getKind() == Kind.VARIABLE) {
            VariableTree variableTree = (VariableTree) tree;
            VariableElement variableElement = TreeUtils.elementFromDeclaration(variableTree);
            if (!PICOTypeUtil.hasOneAndOnlyOneAssignabilityQualifier(variableElement, atypeFactory)) {
                reportFieldMultipleAssignabilityModifiersError(variableElement);
            }
        }
    }

    private void reportFieldMultipleAssignabilityModifiersError(VariableElement field) {
        checker.report(Result.failure("one.assignability.invalid", field), field);
        isValid = false;
    }
}
