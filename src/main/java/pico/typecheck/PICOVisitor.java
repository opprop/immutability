package pico.typecheck;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

/**
 * Created by mier on 20/06/17.
 */
public class PICOVisitor extends BaseTypeVisitor<PICOAnnotatedTypeFactory> {
    public PICOVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        return true;
    }

    /** Ensures immutability modifiers are correctly used */
    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new PICOValidator(checker, this, atypeFactory);
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType invocation, AnnotatedExecutableType constructor, NewClassTree newClassTree) {
        AnnotatedDeclaredType returnType = (AnnotatedDeclaredType) constructor.getReturnType();
        // When an interface is used as the identifier in an anonymous class (e.g. new Comparable() {})
        // the constructor method will be Object.init() {} which has an Object return type
        // When TypeHierarchy attempts to convert it to the supertype (e.g. Comparable) it will return
        // null from asSuper and return false for the check.  Instead, copy the primary annotations
        // to the declared type and then do a subtyping check
        if (invocation.getUnderlyingType().asElement().getKind().isInterface()
                && TypesUtils.isObject(returnType.getUnderlyingType())) {
            final AnnotatedDeclaredType retAsDt = invocation.deepCopy();
            retAsDt.replaceAnnotations(returnType.getAnnotations());
            returnType = retAsDt;
        } else if (newClassTree.getClassBody() != null) {
            // An anonymous class invokes the constructor of its super class, so the underlying
            // types of invocation and returnType are not the same.  Call asSuper so they are the
            // same and the is subtype tests below work correctly
            invocation = AnnotatedTypes.asSuper(atypeFactory, invocation, returnType);
        }

        if (!atypeFactory.getTypeHierarchy().isSubtype(invocation, returnType)) {
            checker.report(Result.failure(
                    "constructor.invocation.invalid",
                    constructor.toString(),
                    invocation,
                    returnType), newClassTree);
            return false;
        }
        return true;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        if (TreeUtils.isConstructor(node)) {
            AnnotatedExecutableType constructorType = atypeFactory.getAnnotatedType(node);
            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) constructorType.getReturnType();
            if (constructorReturnType.hasAnnotation(Readonly.class)) {
                checker.report(Result.failure("consturctor.invalid"), node);
            }
            if (constructorReturnType.hasAnnotation(Immutable.class) ||
                    constructorReturnType.hasAnnotation(PolyImmutable.class)) {
                for (VariableTree paramterTrees : node.getParameters()) {
                    if (atypeFactory.getAnnotatedType(paramterTrees).hasAnnotation(Mutable.class)) {
                        checker.report(Result.failure("consturctor.invalid"), node);
                    }
                }
            }
        }
        return super.visitMethod(node, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(node.getVariable());
        if (receiverType != null && !receiverType.hasAnnotation(Mutable.class)) {
            checker.report(Result.failure("illegal.write"), node);
        }
        return super.visitAssignment(node, p);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement element = TreeUtils.elementFromDeclaration(node);
        if (element != null && element.getKind() == ElementKind.FIELD) {
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(element);
            if (type.hasAnnotation(Mutable.class))
                checker.report(Result.failure("field.mutable.forbidden"), node);
        }
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        // Ensure only @Mutable/@Immutable/@PolyImmutable are used on new instancec creation
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
        if (!(type.hasEffectiveAnnotation(atypeFactory.IMMUTABLE) || type.hasEffectiveAnnotation(atypeFactory.MUTABLE) ||
        type.hasEffectiveAnnotation(atypeFactory.POLYIMMUTABLE))) {
            checker.report(Result.failure("pico.new"), node);
        }
        return super.visitNewClass(node, p);
    }

    /** PICO validator class */
    private class PICOValidator extends BaseTypeValidator {
        public PICOValidator(BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
            checkImmutabilityModifierIsCorrectlyUsed(type, tree);
            return super.visitDeclared(type, tree);
        }

        private void checkImmutabilityModifierIsCorrectlyUsed(AnnotatedTypeMirror type, Tree tree) {
            if (type.getAnnotations().isEmpty() || type.getAnnotations().size() > 1 || type.getAnnotationInHierarchy(PICOVisitor.this.atypeFactory.READONLY) == null) {
                reportError(type, tree);
            }
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Tree tree) {
            checkImmutabilityModifierIsCorrectlyUsed(type, tree);
            return super.visitArray(type, tree);
        }
    }
}
