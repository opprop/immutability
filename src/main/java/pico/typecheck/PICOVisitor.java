package pico.typecheck;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOQualifierHierarchy;
import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

/**
 * Created by mier on 20/06/17.
 */
public class PICOVisitor extends InitializationVisitor<PICOAnnotatedTypeFactory, PICOValue, PICOStore> {
    public PICOVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        return true;
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType invocation, AnnotatedExecutableType constructor, NewClassTree newClassTree) {
        /*Copied Code Start*/
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
        /*Copied Code End*/

        // The immutability return qualifier of the constructor (returnType) must be supertype of the
        // constructor invocation immutability qualifier(invocation).
        AnnotationMirror subATM = invocation.getAnnotationInHierarchy(atypeFactory.READONLY);
        AnnotationMirror superATM = returnType.getAnnotationInHierarchy(atypeFactory.READONLY);
        if (!atypeFactory.getQualifierHierarchy().isSubtype(subATM, superATM)) {
            checker.report(Result.failure(
                    "constructor.invocation.invalid"), newClassTree);
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
        if (receiverType != null && !allowWriteField(receiverType)) {
            checker.report(Result.failure("illegal.field.write"), node);
        }
        return super.visitAssignment(node, p);
    }

    private boolean allowWriteField(AnnotatedTypeMirror receiverType) {
        if (receiverType.hasAnnotation(Mutable.class))
            return true;
        else if (receiverType.hasAnnotation(UnderInitialization.class) && receiverType.hasAnnotation(PolyImmutable.class))
            return true;
        else if (receiverType.hasAnnotation(UnderInitialization.class) && receiverType.hasAnnotation(Immutable.class))
            return true;
        else
            return false;
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
}
