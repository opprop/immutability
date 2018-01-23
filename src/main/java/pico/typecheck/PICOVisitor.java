package pico.typecheck;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import qual.Assignable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.COMMITED;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.POLY_MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

/**
 * Created by mier on 20/06/17.
 * Enforce PICO type rules.
 */
public class PICOVisitor extends InitializationVisitor<PICOAnnotatedTypeFactory, PICOValue, PICOStore> {

    private final boolean shouldOutputFbcError;
    final Map<String, Integer> fbcViolatedMethods;

    public PICOVisitor(BaseTypeChecker checker) {
        super(checker);
        shouldOutputFbcError = checker.getLintOption("printFbcErrors" , false);
        fbcViolatedMethods = shouldOutputFbcError ? new HashMap<>() : null;
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new PICOValidator(checker, this, atypeFactory);
    }

    // This method is for validating usage of mutability qualifier is conformable to element declaration,
    // Ugly thing here is that declarationType is not the result of calling the other method -
    // PICOTypeUtil#getBoundAnnotationOnTypeDeclaration. Instead it's the result of calling ATF#getAnnotatedType(Element).
    // Why it works is that PICOTypeUtil#getBoundAnnotationOnTypeDeclaration and ATF#getAnnotatedType(Element) has
    // the same effect most of the time except on java.lang.Object. We need to be careful when modifying
    // PICOTypeUtil#getBoundAnnotationOnTypeDeclaration so that it has the same behaviour as ATF#getAnnotatedType(Element)
    // (at least for types other than java.lang.Object)
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        AnnotationMirror declared = declarationType.getAnnotationInHierarchy(READONLY);
        // No need to have special case for java.lang.Object, as it's not by default @Readonly anymore
//        if (AnnotationUtils.areSame(declared, atypeFactory.READONLY)) {
//            // Special case for java.lang.Object. Usually @Readonly is never used as a bound annotation for a
//            // TypeElement. But we want to have @Readonly as the default for java.lang.Object. There is no way
//            // of doing this using any exsisting family of @DefaultFor qualifiers, but @ImplicitFor annotation
//            // does the trick. But the side effect is, we can't write @ReceiverDependantMutable, which is the
//            // correct bound for Object element, in jdk.astub, because otherwise it makes all java.lang.Object
//            // to be @ReceiverDependantMutable; Another side effect is here @Readonly is passed into here as
//            // the element type for java.lang.Object. So we have to have this special case only for java.lang.
//            // Object
//            return true;
//        }
        if (AnnotationUtils.areSame(declared, RECEIVER_DEPENDANT_MUTABLE)) {
            // Element is declared with @ReceiverDependantMutable bound, any instantiation is allowed. We don't use
            // a subtype check to validate the correct usage here. Because @Readonly is the super type of
            // @ReceiverDependantMutable, but it's still considered valid usage.
            return true;
        }
        // At this point, element type can only be @Mutable or @Immutable. Otherwise, it's a problem in
        // PICOVisitor#processorClassTree(ClassTree)
        assert AnnotationUtils.areSame(declared, MUTABLE) ||
                AnnotationUtils.areSame(declared, IMMUTABLE);

        // Only forbid incompatible @Mutable and @Immutable between declared and used.
        AnnotationMirror used = useType.getAnnotationInHierarchy(READONLY);
        if (AnnotationUtils.areSame(declared, MUTABLE) && !AnnotationUtils.areSame(used, IMMUTABLE)) {
            return true;
        }

        if (AnnotationUtils.areSame(declared, IMMUTABLE) && !AnnotationUtils.areSame(used, MUTABLE)) {
            return true;
        }

        // All valid cases are listed above. So returns false here.
        return false;
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType invocation, AnnotatedExecutableType constructor, NewClassTree newClassTree) {
        // TODO Is the copied code really needed?
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
        if (!atypeFactory.getTypeHierarchy().isSubtype(invocation, returnType, READONLY)) {
            checker.report(Result.failure(
                    "constructor.invocation.invalid", invocation, returnType), newClassTree);
            return false;
        }
        return true;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(node);
        AnnotationMirror boundAnnotation = PICOTypeUtil.getBoundAnnotationOnEnclosingTypeDeclaration(node, atypeFactory);

        if (TreeUtils.isConstructor(node)) {
            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) executableType.getReturnType();
            if (constructorReturnType.hasAnnotation(READONLY) || constructorReturnType.hasAnnotation(POLY_MUTABLE)) {
                checker.report(Result.failure("constructor.return.invalid", constructorReturnType), node);
                // Immediately go to super implementation if constructor return is not correct to avoid duplicate warnings
                // from "constructor.return.incompatible" if @Readonly or @PolyMutable is used on constructor return
                return super.visitMethod(node, p);
            }

            AnnotationMirror constructorReturnAnnotation = constructorReturnType.getAnnotationInHierarchy(READONLY);

            if (boundAnnotation != null
                    && !AnnotationUtils.areSame(boundAnnotation, RECEIVER_DEPENDANT_MUTABLE)// any constructor return allowed
                    && !atypeFactory.getQualifierHierarchy().isSubtype(constructorReturnAnnotation, boundAnnotation)) {
                checker.report(Result.failure("constructor.return.incompatible"), node);
            }
            /*Doesn't check constructor parameters if constructor return is immutable or receiverdependantmutable*/
        } else {
            AnnotatedDeclaredType declareReceiverType = executableType.getReceiverType();
            if (declareReceiverType != null) {
                AnnotationMirror declaredReceiverAnnotation = declareReceiverType.getAnnotationInHierarchy(READONLY);
                assert declaredReceiverAnnotation != null;// Must be annotated with mutability qualifier. Is this assumption true?
                if (boundAnnotation != null
                        && !AnnotationUtils.areSame(boundAnnotation, RECEIVER_DEPENDANT_MUTABLE)// clone() method doesn't warn
                        && !atypeFactory.getQualifierHierarchy().isSubtype(declaredReceiverAnnotation, boundAnnotation)
                        // Below three are allowed on declared receiver types of instance methods in either @Mutable class or @Immutable class
                        && !AnnotationUtils.areSame(declaredReceiverAnnotation, READONLY)
                        && !AnnotationUtils.areSame(declaredReceiverAnnotation, RECEIVER_DEPENDANT_MUTABLE)
                        && !AnnotationUtils.areSame(declaredReceiverAnnotation, POLY_MUTABLE)) {
                    checker.report(Result.failure("method.receiver.incompatible", declareReceiverType), node);
                }
            }
        }

        flexibleOverrideChecker(node);

        // ObjectIdentityMethod check
        if (PICOTypeUtil.isObjectIdentityMethod(node, atypeFactory)) {
            ObjectIdentityMethodEnforcer.check(atypeFactory.getPath(node.getBody()), atypeFactory, checker);
        }
        return super.visitMethod(node, p);
    }

    private void flexibleOverrideChecker(MethodTree node) {
        // Method overriding checks
        // TODO Copied from super, hence has lots of duplicate code with super. We need to
        // change the signature of checkOverride() method to also pass ExecutableElement for
        // viewpoint adaptation.
        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);
        AnnotatedDeclaredType enclosingType =
                (AnnotatedDeclaredType)
                        atypeFactory.getAnnotatedType(methodElement.getEnclosingElement());

        Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
                AnnotatedTypes.overriddenMethods(elements, atypeFactory, methodElement);
        for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
                overriddenMethods.entrySet()) {
            AnnotatedDeclaredType overriddenType = pair.getKey();
            AnnotatedExecutableType overriddenMethod =
                    AnnotatedTypes.asMemberOf(
                            types, atypeFactory, enclosingType, pair.getValue(), node);
            // Viewpoint adapt super method executable type to current class bound(is this always class bound?)
            // to allow flexible overriding
            atypeFactory.viewpointAdaptMethod(pair.getValue(), enclosingType, overriddenMethod);
            AnnotatedExecutableType overrider = atypeFactory.getAnnotatedType(node);
            if (!checkOverride(node, overrider, enclosingType, overriddenMethod, overriddenType)) {
                // Stop at the first mismatch; this makes a difference only if
                // -Awarns is passed, in which case multiple warnings might be raised on
                // the same method, not adding any value. See Issue 373.
                break;
            }
        }
    }

    // Disables method overriding checks in BaseTypeVisitor
    @Override
    protected boolean checkOverride(
            MethodTree overriderTree, AnnotatedDeclaredType overridingType,
            AnnotatedExecutableType overridden, AnnotatedDeclaredType overriddenType) {
        return true;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        ExpressionTree variable = node.getVariable();
        // TODO Question Here, receiver type uses flow refinement. But in commonAssignmentCheck to compute lhs type
        // , it doesn't. This causes inconsistencies when enforcing immutability and doing subtype check. I overrode
        // getAnnotatedTypeLhs() to also use flow sensitive refinement, but came across with "private access" problem
        // on field "computingAnnotatedTypeMirrorOfLHS"
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(variable);
        // Cannot use receiverTree = TreeUtils.getReceiverTree(variable) to determine if it's
        // field assignment or not. Because for field assignment with implicit "this", receiverTree
        // is null but receiverType is non-null. We still need to check this case.
        if (receiverType == null) {
            return super.visitAssignment(node, p);
        }
        if (!allowWrite(receiverType, node)) {
            reportFieldOrArrayWriteError(node, variable, receiverType);
        }
        return super.visitAssignment(node, p);
    }

    private boolean allowWrite(AnnotatedTypeMirror receiverType, AssignmentTree node) {
        // One pico side, if only receiver is mutable, we allow assigning/reassigning. Because if the field
        // is declared as final, Java compiler will catch that, and we couldn't have reached this point
        if (PICOTypeUtil.isAssigningAssignableField(node, atypeFactory)) {
            return isAllowedAssignableField(receiverType, node);
        } else if (isInitializingReceiverDependantMutableOrImmutableObject(receiverType)) {
            return true;
        } else if (receiverType.hasAnnotation(MUTABLE)) {
            return true;
        }

        return false;
    }

    private boolean isAllowedAssignableField(AnnotatedTypeMirror receiverType, AssignmentTree node) {
        Element fieldElement = TreeUtils.elementFromUse(node);
        AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(fieldElement);
        if (fieldElement == null) return false;
        // Forbid the case that might break type soundness
        return !(receiverType.hasAnnotation(READONLY) && fieldType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE));
    }

    private boolean isInitializingReceiverDependantMutableOrImmutableObject(AnnotatedTypeMirror receiverType) {
        if (receiverType.hasAnnotation(UnderInitialization.class) && receiverType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
            return true;
        } else if (receiverType.hasAnnotation(UnderInitialization.class) && receiverType.hasAnnotation(IMMUTABLE)) {
            return true;
        } else {
            return false;
        }
    }

    private void reportFieldOrArrayWriteError(AssignmentTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        if (variable.getKind() == Kind.MEMBER_SELECT) {
            checker.report(Result.failure("illegal.field.write", receiverType), TreeUtils.getReceiverTree(variable));
        } else if (variable.getKind() == Kind.IDENTIFIER) {
            checker.report(Result.failure("illegal.field.write", receiverType), node);
        } else if (variable.getKind() == Kind.ARRAY_ACCESS) {
            checker.report(Result.failure("illegal.array.write", receiverType), ((ArrayAccessTree)variable).getExpression());
        } else {
            ErrorReporter.errorAbort("Unknown assignment variable at: ", node);
        }
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement element = TreeUtils.elementFromDeclaration(node);
        if (element != null && element.getKind() == ElementKind.FIELD) {
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(element);
            if (type.hasAnnotation(POLY_MUTABLE)) {
                checker.report(Result.failure("field.polymutable.forbidden", element), node);
            }
        }
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        checkNewInstanceCreation(node);
        return super.visitNewClass(node, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        checkNewInstanceCreation(node);
        return super.visitNewArray(node, p);
    }

    private void checkNewInstanceCreation(Tree node) {
        // Ensure only @Mutable/@Immutable/@ReceiverDependantMutable/@PolyMutable are used on new instance creation
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
        if (!(type.hasAnnotation(IMMUTABLE) || type.hasAnnotation(MUTABLE) ||
        type.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE) || type.hasAnnotation(POLY_MUTABLE))) {
            checker.report(Result.failure("pico.new.invalid", type), node);
        }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        super.visitMethodInvocation(node, p);
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =
                atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.first;
        ExecutableElement invokedMethodElement = invokedMethod.getElement();
        // Only check invocability if it's super call, as non-super call is already checked
        // by super implementation(of course in both cases, invocability is not checked when
        // invoking static methods)
        if (!ElementUtils.isStatic(invokedMethodElement) && TreeUtils.isSuperCall(node)) {
            checkMethodInvocability(invokedMethod, node);
        }
        return null;
    }

    // TODO Find a better way to inject saveFbcViolatedMethods instead of copying lots of code from super method
    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method, MethodInvocationTree node) {
        // Check subclass constructor calls the correct super class constructor: mutable calls mutable; immutable
        // calls immutable; any calls receiverdependantmutable
        if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            AnnotatedTypeMirror subClassConstructorReturnType = atypeFactory.getReceiverType(node);
            AnnotatedTypeMirror superClassConstructorReturnType = method.getReturnType();
            // superClassConstructorReturnType is already the result of viewpoint adaptation, so subClassConstructorReturnType <:
            // superClassConstructorReturnType is enough to determine the super constructor invocation is valid or not
            if (!atypeFactory.getTypeHierarchy().isSubtype(subClassConstructorReturnType, superClassConstructorReturnType, READONLY)) {
                checker.report(
                        Result.failure(
                                "super.constructor.invocation.incompatible", subClassConstructorReturnType, superClassConstructorReturnType), node);
            }
        }

        /*Copied Code Starts*/
        if (method.getReceiverType() == null) {
            // Static methods don't have a receiver.
            return;
        }

        AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
        AnnotatedTypeMirror treeReceiver = methodReceiver.shallowCopy(false);
        AnnotatedTypeMirror rcv = atypeFactory.getReceiverType(node);

        treeReceiver.addAnnotations(rcv.getEffectiveAnnotations());

        if (!skipReceiverSubtypeCheck(node, methodReceiver, rcv)
                && !atypeFactory.getTypeHierarchy().isSubtype(treeReceiver, methodReceiver)) {
            checker.report(
                    Result.failure(
                            "method.invocation.invalid",
                            TreeUtils.elementFromUse(node),
                            treeReceiver.toString(),
                            methodReceiver.toString()),
                    node);
            /*Difference Starts*/
            if (shouldOutputFbcError) {
                saveFbcViolatedMethods(TreeUtils.elementFromUse(node), treeReceiver.toString(), methodReceiver.toString());
            }
            /*Different Ends*/
        }
        /*Copied Code Ends*/
    }

    private void saveFbcViolatedMethods(ExecutableElement method, String actualReceiver, String declaredReceiver) {
        if (actualReceiver.contains("@UnderInitialization") && declaredReceiver.contains("@Initialized")) {
            String key = ElementUtils.enclosingClass(method) + "#" + method;
            Integer times = fbcViolatedMethods.get(key) == null ? 1 : fbcViolatedMethods.get(key) + 1;
            fbcViolatedMethods.put(key, times);
        }
    }

    @Override
    protected void checkFieldsInitialized(Tree blockNode, boolean staticFields, PICOStore store, List<? extends AnnotationMirror> receiverAnnotations) {
        // If a class doesn't have constructor, it cannot be initialized as @Immutable, therefore no need to check uninitialized fields
        if (TreeUtils.isClassTree(blockNode)) return;
        if (blockNode.getKind() == Kind.METHOD && TreeUtils.isConstructor((MethodTree)blockNode)) {
            // Only raise errors when in @Immutable or @ReceiverDependantMutable constructors. As @Mutable constructor can initialized
            // those fields out of constructor
            MethodTree methodTree = (MethodTree) blockNode;
            AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(methodTree);
            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) executableType.getReturnType();
            // Only care abstract state initialization in @Immutable and @ReceiverDependantMutable constructors, as @Mutable constructors
            // only allows instantiating @Mutable objects and fields can be initialized later
            if (!(constructorReturnType.hasAnnotation(IMMUTABLE) || constructorReturnType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE))) {
                return;
            }
        }
        super.checkFieldsInitialized(blockNode, staticFields, store, receiverAnnotations);
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(atypeFactory.getQualifierHierarchy().getBottomAnnotation(BOTTOM));
        result.add(COMMITED);
        return result;
    }

    @Override
    protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(atypeFactory.getQualifierHierarchy().getTopAnnotation(READONLY));
        result.add(COMMITED);
        return result;
    }

    @Override
    public void processClassTree(ClassTree node) {
        TypeElement typeElement = TreeUtils.elementFromDeclaration(node);
        AnnotationMirror bound = PICOTypeUtil.getBoundAnnotationOnTypeDeclaration(typeElement, atypeFactory);
        // Skip bound validation for anonymous classes(whose bound is null)
        if (bound != null) {
            // Has to be either @Mutable, @ReceiverDependantMutable or @Immutable, nothing else
            if (!AnnotationUtils.areSame(bound, MUTABLE)
                    && !AnnotationUtils.areSame(bound, RECEIVER_DEPENDANT_MUTABLE)
                    && !AnnotationUtils.areSame(bound, IMMUTABLE)) {
                checker.report(Result.failure(
                        "class.bound.invalid", bound), node);
                return;// Doesn't process the class tree anymore
            }
            // Must have compatible bound annotation as the direct super types
            List<AnnotationMirror> superBounds = PICOTypeUtil.getBoundAnnotationOnDirectSuperTypeDeclarations(typeElement, atypeFactory);
            for (AnnotationMirror superBound : superBounds) {
                // If annotation on super bound is @ReceiverDependantMutable, then any valid bound is permitted.
                if (AnnotationUtils.areSame(superBound, RECEIVER_DEPENDANT_MUTABLE)) continue;
                // super bound is either @Mutable or @Immutable. Must be the subtype of the corresponding super bound
                if (!atypeFactory.getQualifierHierarchy().isSubtype(bound, superBound)) {
                    checker.report(Result.failure(
                            "subclass.bound.incompatible", bound, superBound), node);
                    return;
                }
            }
        }
        // Reach this point iff bound annotation is one of mutable, rdm or immutable;
        // and bound is compatible with bounds on super types or the current class is
        // anonymous class
        super.processClassTree(node);
    }
}
