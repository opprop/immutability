package pico.typecheck;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.List;

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
    public Void visitTypeParameter(TypeParameterTree node, Void p) {
        AnnotatedTypeMirror upperBound = atypeFactory.getAnnotatedTypeFromTypeTree(node);
        // Ensure upper bound has effective immutable upper bound if the enclosing class has @Immutable on declaration
        boolean hasImmutableBoundAnnotation = hasImmutableAnnotationOnTypeDeclaration(node);
        if (hasImmutableBoundAnnotation &&
                !AnnotationUtils.areSameByClass(upperBound.getEffectiveAnnotationInHierarchy(atypeFactory.READONLY), Immutable.class)) {
            checker.report(Result.failure("immutable.class.type.parameter.bound.invalid"), node);
        }
        return super.visitTypeParameter(node, p);
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
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(node);
        boolean hasImmutableBoundAnnotation = hasImmutableAnnotationOnTypeDeclaration(node);

        if (TreeUtils.isConstructor(node)) {
            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) executableType.getReturnType();
            if (constructorReturnType.hasAnnotation(Readonly.class)) {
                checker.report(Result.failure("constructor.invalid"), node);
            }

            AnnotationMirror constructorReturnAnnotation = constructorReturnType.getAnnotationInHierarchy(atypeFactory.READONLY);

            if (hasImmutableBoundAnnotation
                    && !atypeFactory.getQualifierHierarchy().isSubtype(constructorReturnAnnotation, atypeFactory.IMMUTABLE)) {
                checker.report(Result.failure("immutable.class.constructor.invalid"), node);
            }
            if (constructorReturnType.hasAnnotation(Immutable.class) ||
                    constructorReturnType.hasAnnotation(PolyImmutable.class)) {
                for (VariableTree paramterTrees : node.getParameters()) {
                    if (atypeFactory.getAnnotatedType(paramterTrees).hasAnnotation(Mutable.class)
                            || atypeFactory.getAnnotatedType(paramterTrees).hasAnnotation(Readonly.class)) {
                        checker.report(Result.failure("constructor.invalid"), node);
                    }
                }
            }
        } else {
            AnnotatedDeclaredType declareReceiverType = executableType.getReceiverType();
            if (hasImmutableBoundAnnotation && declareReceiverType != null &&
                    !(declareReceiverType.hasAnnotation(atypeFactory.READONLY) ||
                            declareReceiverType.hasAnnotation(atypeFactory.IMMUTABLE))) {
                checker.report(Result.failure("immutable.class.method.receiver.invalid"), node);
            }
        }
        return super.visitMethod(node, p);
    }

    private boolean hasImmutableAnnotationOnTypeDeclaration(Tree node) {
        TypeElement typeElement = null;
        if (node instanceof MethodTree) {
            MethodTree methodTree = (MethodTree) node;
            ExecutableElement element = TreeUtils.elementFromDeclaration(methodTree);
            typeElement = ElementUtils.enclosingClass(element);
        } else if (node instanceof TypeParameterTree) {
            TypeParameterTree typeParameterTree = (TypeParameterTree) node;
            TreePath treePath = atypeFactory.getPath(typeParameterTree);
            ClassTree classTree = TreeUtils.enclosingClass(treePath);
            // This means type parameter is declared not on type declaration, but on generic methods
            if (classTree != treePath.getParentPath().getLeaf()) {
                return false;
            }
            typeElement = TreeUtils.elementFromDeclaration(classTree);
        }
        if (typeElement == null) {
            ErrorReporter.errorAbort("Enclosing typeElement should not be null!", node);
        }
        AnnotatedTypeMirror bound = atypeFactory.fromElement(typeElement);
        AnnotationMirror boundAnnotation = bound.getAnnotationInHierarchy(atypeFactory.READONLY);
        return boundAnnotation != null
                && AnnotationUtils.areSameByClass(boundAnnotation, Immutable.class);

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

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        // Skip calls to the Enum constructor (they're generated by javac and
        // hard to check), also see CFGBuilder.visitMethodInvocation.
            if (TreeUtils.elementFromUse(node) == null || TreeUtils.isEnumSuper(node)) {
            return super.visitMethodInvocation(node, p);
        }

        if (shouldSkipUses(node)) {
            return super.visitMethodInvocation(node, p);
        }

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =
                atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.first;
        List<AnnotatedTypeMirror> typeargs = mfuPair.second;

        if (!atypeFactory.ignoreUninferredTypeArguments) {
            for (AnnotatedTypeMirror typearg : typeargs) {
                if (typearg.getKind() == TypeKind.WILDCARD
                        && ((AnnotatedWildcardType) typearg).isUninferredTypeArgument()) {
                    checker.report(
                            Result.failure(
                                    "type.arguments.not.inferred",
                                    invokedMethod.getElement().getSimpleName()),
                            node);
                    break; // only issue error once per method
                }
            }
        }

        List<AnnotatedTypeParameterBounds> paramBounds = new ArrayList<>();
        for (AnnotatedTypeVariable param : invokedMethod.getTypeVariables()) {
            paramBounds.add(param.getBounds());
        }

        checkTypeArguments(node, paramBounds, typeargs, node.getTypeArguments());

        List<AnnotatedTypeMirror> params =
                AnnotatedTypes.expandVarArgs(atypeFactory, invokedMethod, node.getArguments());
        checkArguments(params, node.getArguments());

        if (isVectorCopyInto(invokedMethod)) {
            typeCheckVectorCopyIntoArgument(node, params);
        }

        ExecutableElement invokedMethodElement = invokedMethod.getElement();
        /*** Only code below is different from super implementation ***/
        if (!ElementUtils.isStatic(invokedMethodElement)) {
            checkMethodInvocability(invokedMethod, node);
        }
        /*** Only code above is different from super implementation ***/

        // check precondition annotations
        checkPreconditions(node, contractsUtils.getPreconditions(invokedMethodElement));

        // Do not call super, as that would observe the arguments without
        // a set assignment context.
        scan(node.getMethodSelect(), p);
        return null; // super.visitMethodInvocation(node, p);
    }

    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method, MethodInvocationTree node) {
        if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            AnnotatedTypeMirror subClassConstructorReturnType = atypeFactory.getReceiverType(node);
            AnnotatedTypeMirror superClassConstructorReturnType = method.getReturnType();
            if (!superClassConstructorReturnType.hasAnnotation(Mutable.class)
            && !atypeFactory.getTypeHierarchy().isSubtype(subClassConstructorReturnType, superClassConstructorReturnType)) {
                checker.report(
                        Result.failure(
                                "constructor.invocation.invalid",
                                TreeUtils.elementFromUse(node),
                                subClassConstructorReturnType.toString(),
                                superClassConstructorReturnType.toString()),
                        node);
            }
        }

        super.checkMethodInvocability(method, node);
    }
}
