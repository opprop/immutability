package pico.typecheck;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.COMMITED;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.POLY_MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import pico.common.ExtendedViewpointAdapter;
import pico.common.PICOTypeUtil;
import pico.common.ViewpointAdapterGettable;

/**
 * Created by mier on 20/06/17.
 * Enforce PICO type rules.
 */
public class PICOVisitor extends InitializationVisitor<PICOAnnotatedTypeFactory, PICOValue, PICOStore> {

    private final boolean shouldOutputFbcError;
    final Map<String, Integer> fbcViolatedMethods;

    public PICOVisitor(BaseTypeChecker checker) {
        super(checker);
        shouldOutputFbcError = checker.hasOption("printFbcErrors");
        fbcViolatedMethods = shouldOutputFbcError ? new HashMap<>() : null;
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new PICOValidator(checker, this, atypeFactory);
    }

    // This method is for validating usage of mutability qualifier is conformable to element declaration,
    // Ugly thing here is that declarationType is not the result of calling the other method -
    // PICOTypeUtil#getBoundTypeOfTypeDeclaration. Instead it's the result of calling ATF#getAnnotatedType(Element).
    // Why it works is that PICOTypeUtil#getBoundTypeOfTypeDeclaration and ATF#getAnnotatedType(Element) has
    // the same effect most of the time except on java.lang.Object. We need to be careful when modifying
    // PICOTypeUtil#getBoundTypeOfTypeDeclaration so that it has the same behaviour as ATF#getAnnotatedType(Element)
    // (at least for types other than java.lang.Object)
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        // FIXME workaround for typecheck BOTTOM
        if (useType.hasAnnotation(BOTTOM)) {
            return true;
        }

        AnnotationMirror declared = declarationType.getAnnotationInHierarchy(READONLY);
        AnnotationMirror used = useType.getAnnotationInHierarchy(READONLY);

        return isAdaptedSubtype(used, declared);
    }

    static private boolean isAnnoValidUse(AnnotationMirror declared, AnnotationMirror used) {
        if (AnnotationUtils.areSame(declared, RECEIVER_DEPENDANT_MUTABLE) || AnnotationUtils.areSame(declared, READONLY)) {
            // Element is declared with @ReceiverDependantMutable bound, any instantiation is allowed. We don't use
            // a subtype check to validate the correct usage here. Because @Readonly is the super type of
            // @ReceiverDependantMutable, but it's still considered valid usage.
            return true;
        }

        if (AnnotationUtils.areSame(declared, MUTABLE) &&
                !(AnnotationUtils.areSame(used, IMMUTABLE) || AnnotationUtils.areSame(used, RECEIVER_DEPENDANT_MUTABLE))) {
            return true;
        }

        if (AnnotationUtils.areSame(declared, IMMUTABLE) &&
                !(AnnotationUtils.areSame(used, MUTABLE) || AnnotationUtils.areSame(used, RECEIVER_DEPENDANT_MUTABLE))) {
            return true;
        }

        // All valid cases are listed above. So returns false here.
        return false;
    }

    private boolean isAdaptedSubtype(AnnotationMirror lhs, AnnotationMirror rhs) {
        ExtendedViewpointAdapter vpa = ((ViewpointAdapterGettable)atypeFactory).getViewpointAdapter();
        AnnotationMirror adapted = vpa.rawCombineAnnotationWithAnnotation(lhs, rhs);
        return atypeFactory.getQualifierHierarchy().isSubtype(adapted, lhs);
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, String errorKey) {
        AnnotatedTypeMirror var = atypeFactory.getAnnotatedTypeLhs(varTree);
        assert var != null : "no variable found for tree: " + varTree;

        if (!validateType(varTree, var)) {
            return;
        }

        if (varTree instanceof VariableTree) {
            VariableElement element = TreeUtils.elementFromDeclaration((VariableTree) varTree);
            if (element.getKind() == ElementKind.FIELD && !ElementUtils.isStatic(element)) {
                AnnotatedDeclaredType bound = PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(varTree, atypeFactory);
                // var is singleton, so shouldn't modify var directly. Otherwise, the variable tree's type will be
                // altered permanently, and other clients who access this type will see the change, too.
                AnnotatedTypeMirror varAdapted = var.shallowCopy(true);
                // Viewpoint adapt varAdapted to the bound. PICOInferenceAnnotatedTypeFactory#viewpointAdaptMember()
                // mutates varAdapted, so after the below method is called, varAdapted is the result adapted to bound
                atypeFactory.getViewpointAdapter().viewpointAdaptMember(bound, element, varAdapted);
                // Pass varAdapted here as lhs type.
                // Caution: cannot pass var directly. Modifying type in PICOInferenceTreeAnnotator#
                // visitVariable() will cause wrong type to be gotton here, as on inference side,
                // atm is uniquely determined by each element.
                commonAssignmentCheck(varAdapted, valueExp, errorKey);
                return;
            }
        }

        commonAssignmentCheck(var, valueExp, errorKey);
    }



    @Override
    protected void checkConstructorInvocation(AnnotatedDeclaredType invocation, AnnotatedExecutableType constructor, NewClassTree newClassTree) {
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

        // TODO fix inference counterpart, not here
//        // CF base check disabled by InitializationVisitor
//        // if no explicit anno it must inherited from class decl
//        AnnotationMirror declAnno = constructor.getReturnType().getAnnotationInHierarchy(READONLY);
//        AnnotationMirror useAnno = invocation.getAnnotationInHierarchy(READONLY);
//        declAnno = declAnno == null ? MUTABLE : declAnno;
//
//        if(useAnno != null && !AnnotationUtils.areSameByName(declAnno, POLY_MUTABLE) && !isAdaptedSubtype(useAnno, declAnno)) {
//            checker.report(Result.failure("type.invalid.annotations.on.use", declAnno, useAnno), newClassTree);
//        }

        // The immutability return qualifier of the constructor (returnType) must be supertype of the
        // constructor invocation immutability qualifier(invocation).
        if (!atypeFactory.getTypeHierarchy().isSubtype(invocation, returnType, READONLY)) {
            checker.report(Result.failure(
                    "constructor.invocation.invalid", invocation, returnType), newClassTree);
        }
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(node);
        AnnotatedDeclaredType bound = PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(node, atypeFactory);

        if (TreeUtils.isConstructor(node)) {
            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) executableType.getReturnType();
            if (constructorReturnType.hasAnnotation(READONLY) || constructorReturnType.hasAnnotation(POLY_MUTABLE)) {
                checker.report(Result.failure("constructor.return.invalid", constructorReturnType), node);
                return super.visitMethod(node, p);
            }
            // if no explicit anno it must inherit from class decl so identical
            // => if not the same must not inherited from class decl
            // => no need to check the source of the anno

        } else {
            AnnotatedDeclaredType declareReceiverType = executableType.getReceiverType();
            if (declareReceiverType != null) {
                if (bound != null
                        && !bound.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)
                        && !atypeFactory.getQualifierHierarchy().isSubtype(
                                declareReceiverType.getAnnotationInHierarchy(READONLY),
                                bound.getAnnotationInHierarchy(READONLY))
                        // Below three are allowed on declared receiver types of instance methods in either @Mutable class or @Immutable class
                        && !declareReceiverType.hasAnnotation(READONLY)
                        && !declareReceiverType.hasAnnotation(POLY_MUTABLE)) {
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
                            types, atypeFactory, enclosingType, pair.getValue());
            // Viewpoint adapt super method executable type to current class bound(is this always class bound?)
            // to allow flexible overriding
            atypeFactory.getViewpointAdapter().viewpointAdaptMethod(enclosingType, pair.getValue() , overriddenMethod);
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
        checkMutation(node, variable);
        return super.visitAssignment(node, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        ExpressionTree variable = node.getVariable();
        checkMutation(node, variable);
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        if (PICOTypeUtil.isSideEffectingUnaryTree(node)) {
            ExpressionTree variable = node.getExpression();
            checkMutation(node, variable);
        }
        return super.visitUnary(node, p);
    }

    private void checkMutation(Tree node, ExpressionTree variable) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(variable);
        // Cannot use receiverTree = TreeUtils.getReceiverTree(variable) to determine if it's
        // field assignment or not. Because for field assignment with implicit "this", receiverTree
        // is null but receiverType is non-null. We still need to check this case.
        if (receiverType != null && !allowWrite(receiverType, variable)) {
            reportFieldOrArrayWriteError(node, variable, receiverType);
        }
    }

    private boolean allowWrite(AnnotatedTypeMirror receiverType, ExpressionTree variable) {
        // One pico side, if only receiver is mutable, we allow assigning/reassigning. Because if the field
        // is declared as final, Java compiler will catch that, and we couldn't have reached this point
        if (PICOTypeUtil.isAssigningAssignableField(variable, atypeFactory)) {
            return isAllowedAssignableField(receiverType, variable);
        } else if (isInitializingReceiverDependantMutableOrImmutableObject(receiverType)) {
            return true;
        } else if (receiverType.hasAnnotation(MUTABLE)) {
            return true;
        }

        return false;
    }

    private boolean isAllowedAssignableField(AnnotatedTypeMirror receiverType, ExpressionTree node) {
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
        } else if (receiverType.hasAnnotation(UnderInitialization.class) && receiverType.hasAnnotation(MUTABLE)) {
            return true;
        } else {
            return false;
        }
    }

    private void reportFieldOrArrayWriteError(Tree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        if (variable.getKind() == Kind.MEMBER_SELECT) {
            checker.report(Result.failure("illegal.field.write", receiverType), TreeUtils.getReceiverTree(variable));
        } else if (variable.getKind() == Kind.IDENTIFIER) {
            checker.report(Result.failure("illegal.field.write", receiverType), node);
        } else if (variable.getKind() == Kind.ARRAY_ACCESS) {
            checker.report(Result.failure("illegal.array.write", receiverType), ((ArrayAccessTree)variable).getExpression());
        } else {
            throw new BugInCF("Unknown assignment variable at: ", node);
        }
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement element = TreeUtils.elementFromDeclaration(node);
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(element);
        if (element.getKind() == ElementKind.FIELD) {
            if (type.hasAnnotation(POLY_MUTABLE)) {
                checker.report(Result.failure("field.polymutable.forbidden", element), node);
            }
        }
        checkAndReportInvalidAnnotationOnUse(type, node);
        return super.visitVariable(node, p);
    }

    private void checkAndReportInvalidAnnotationOnUse(AnnotatedTypeMirror type, Tree node) {
        AnnotationMirror useAnno = type.getAnnotationInHierarchy(READONLY);
        if (useAnno != null && !PICOTypeUtil.isImplicitlyImmutableType(type) && type.getKind() != TypeKind.ARRAY) {  // TODO: annotate the use instead of using this
            AnnotationMirror defaultAnno = MUTABLE;
            for (AnnotationMirror anno : atypeFactory.getTypeDeclarationBounds(atypeFactory.getAnnotatedType(node).getUnderlyingType())) {
                if (atypeFactory.getQualifierHierarchy().isSubtype(anno, READONLY) && !AnnotationUtils.areSame(anno, READONLY)) {
                    defaultAnno = anno;
                }
            }
            if (!isAdaptedSubtype(useAnno, defaultAnno)) {
                checker.report(Result.failure("type.invalid.annotations.on.use", defaultAnno, useAnno), node);
            }
        }
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
        ParameterizedExecutableType mfuPair =
                atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.executableType;
        ExecutableElement invokedMethodElement = invokedMethod.getElement();
        // Only check invocability if it's super call, as non-super call is already checked
        // by super implementation(of course in both cases, invocability is not checked when
        // invoking static methods)
        if (!ElementUtils.isStatic(invokedMethodElement) && TreeUtils.isSuperConstructorCall(node)) {
            checkMethodInvocability(invokedMethod, node);
        }
        return null;
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
        // TODO Don't process anonymous class. I'm not even sure if whether processClassTree(ClassTree) is
        // called on anonymous class tree
        if (typeElement.toString().contains("anonymous")) {
            super.processClassTree(node);
            return;
        }

        AnnotatedDeclaredType bound = PICOTypeUtil.getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
        // Has to be either @Mutable, @ReceiverDependantMutable or @Immutable, nothing else
        if (!bound.hasAnnotation(MUTABLE) && !bound.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE) && !bound.hasAnnotation(IMMUTABLE)) {
            checker.report(Result.failure("class.bound.invalid", bound), node);
            return;// Doesn't process the class tree anymore
        }

        super.processClassTree(node);
    }
    
    @Override
    protected void checkExtendsImplements(ClassTree classTree) {
        // validateTypeOf does not check super trees
        PICOAnnotatedTypeFactory.PICOQualifierForUseTypeAnnotator annotator = new PICOAnnotatedTypeFactory.PICOQualifierForUseTypeAnnotator(atypeFactory);
    	if (TypesUtils.isAnonymous(TreeUtils.typeOf(classTree))) {
            // Don't check extends clause on anonymous classes.
            return;
        }

    	Tree extendsClause = classTree.getExtendsClause();
    	if (extendsClause != null) {

    	}

    	List<? extends Tree> implementsClauses = classTree.getImplementsClause();
    	if (implementsClauses != null) {
    		for (Tree impl : implementsClauses) {

    		}
    	}
    }

    /**
     * The invoked constructor’s return type adapted to the invoking constructor’s return type must
     * be a supertype of the invoking constructor’s return type. Since InitializationChecker does not
     * apply any type rules at here, only READONLY hierarchy is checked.
     *
     * @param superCall the super invocation, e.g., "super()"
     * @param errorKey the error key, e.g., "super.invocation.invalid"
     */
    @Override
    protected void checkThisOrSuperConstructorCall(
            MethodInvocationTree superCall, @CompilerMessageKey String errorKey) {
        MethodTree enclosingMethod = visitorState.getMethodTree();
        AnnotatedTypeMirror superType = atypeFactory.getAnnotatedType(superCall);
        AnnotatedExecutableType constructorType = atypeFactory.getAnnotatedType(enclosingMethod);
        AnnotationMirror superTypeMirror = superType.getAnnotationInHierarchy(READONLY);
        AnnotationMirror constructorTypeMirror =
                constructorType.getReturnType().getAnnotationInHierarchy(READONLY);
        if (!atypeFactory
                .getQualifierHierarchy()
                .isSubtype(constructorTypeMirror, superTypeMirror)) {
            checker.report(
                    Result.failure(errorKey, constructorTypeMirror, superCall, superTypeMirror),
                    superCall);
        }
        super.checkThisOrSuperConstructorCall(superCall, errorKey);
    }
}
