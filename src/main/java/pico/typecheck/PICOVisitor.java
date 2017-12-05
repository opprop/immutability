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
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
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
import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

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

    /**No need to check usage type is subtype of the declaration type*/
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
            checker.report(Result.failure("immutable.class.type.parameter.bound.invalid",
                    upperBound.getEffectiveAnnotationInHierarchy(atypeFactory.READONLY)), node);
        }
        return super.visitTypeParameter(node, p);
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
        if (!atypeFactory.getTypeHierarchy().isSubtype(invocation, returnType, atypeFactory.READONLY)) {
            checker.report(Result.failure(
                    "constructor.invocation.invalid", invocation, returnType), newClassTree);
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
            if (constructorReturnType.hasAnnotation(Readonly.class) || constructorReturnType.hasAnnotation(PolyMutable.class)) {
                checker.report(Result.failure("constructor.return.invalid", constructorReturnType), node);
            }

            AnnotationMirror constructorReturnAnnotation = constructorReturnType.getAnnotationInHierarchy(atypeFactory.READONLY);

            if (hasImmutableBoundAnnotation
                    && !atypeFactory.getQualifierHierarchy().isSubtype(constructorReturnAnnotation, atypeFactory.IMMUTABLE)) {
                checker.report(Result.failure("immutable.class.constructor.invalid"), node);
            }
            /*Doesn't check constructor parameters if constructor return is immutable or receiverdependantmutable*/
        } else {
            AnnotatedDeclaredType declareReceiverType = executableType.getReceiverType();
            if (hasImmutableBoundAnnotation && declareReceiverType != null &&
                    !(declareReceiverType.hasAnnotation(atypeFactory.READONLY) ||
                            declareReceiverType.hasAnnotation(atypeFactory.IMMUTABLE))) {
                checker.report(Result.failure("immutable.class.method.receiver.invalid"), node.getReceiverParameter());
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
        // Ignore anonymous classes. It doesn't have bound annotation. The annotation on new instance
        // creation is mis-passed here as bound annotation. As a result, if anonymous class is instantiated
        // with @Immutable instance, it gets warned "immutable.class.constructor.invalid" because anonymous
        // class only has @Mutable constructor
        if (typeElement.toString().contains("anonymous")) return false;
        AnnotatedTypeMirror bound = atypeFactory.fromElement(typeElement);
        AnnotationMirror boundAnnotation = bound.getAnnotationInHierarchy(atypeFactory.READONLY);
        return boundAnnotation != null
                && AnnotationUtils.areSameByClass(boundAnnotation, Immutable.class);

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
        // If receiver != null, it's field assignment
        if (!allowWrite(receiverType, node)) {
            reportFieldOrArrayWriteError(node, variable, receiverType);
        }
        return super.visitAssignment(node, p);
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

    private boolean allowWrite(AnnotatedTypeMirror receiverType, AssignmentTree node) {
        // One pico side, if only receiver is mutable, we allow assigning/reassigning. Because if the field
        // is declared as final, Java compiler will catch that, and we couldn't have reached this point
        if (receiverType.hasAnnotation(Mutable.class)) {
            return true;
        } else if (isInitializingReceiverDependantMutableOrImmutableObject(receiverType)) {
            return true;
        } else if (isAssigningAssignableField(receiverType, node)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInitializingReceiverDependantMutableOrImmutableObject(AnnotatedTypeMirror receiverType) {
        if (receiverType.hasAnnotation(UnderInitialization.class) && receiverType.hasAnnotation(ReceiverDependantMutable.class)) {
            return true;
        } else if (receiverType.hasAnnotation(UnderInitialization.class) && receiverType.hasAnnotation(Immutable.class)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAssigningAssignableField(AnnotatedTypeMirror receiverType, AssignmentTree node) {
        Element fieldElement = TreeUtils.elementFromUse(node);
        if (fieldElement == null) return false;
        AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(fieldElement);
        // Forbid the case that might break type soundness
        if (isAssignableField(fieldElement) && receiverType.hasAnnotation(Readonly.class)
                && fieldType.hasAnnotation(ReceiverDependantMutable.class)) {
            return false;
        } else if (isAssignableField(fieldElement)) {
            return true;
        }
        return false;
    }

    /**Util methods to determine fields' assignability*/
    /**Check if a field is assignable or not.*/
    protected boolean isAssignableField(Element variableElement) {
        assert variableElement instanceof VariableElement;
        boolean hasExplicitAssignableAnnotation = atypeFactory.getDeclAnnotation(variableElement, Assignable.class) != null;
        if (!ElementUtils.isStatic(variableElement)) {
            // Instance fields must have explicit @Assignable annotation to be assignable
            return hasExplicitAssignableAnnotation;
        } else {
            // If there is explicit @Assignable annotation on static fields, then it's assignable; If there isn't,
            // and the static field is not final, we treat it as if it's assignable field.
            return hasExplicitAssignableAnnotation || !isFinalField(variableElement);
        }
    }

    /**Check if a field is final or not.*/
    protected boolean isFinalField(Element variableElement) {
        assert variableElement instanceof VariableElement;
        return ElementUtils.isFinal(variableElement);
    }

    /**Check if a field is @ReceiverDependantAssignable. Static fields always returns false.*/
    protected boolean isReceiverDependantAssignable(Element variableElement) {
        assert variableElement instanceof VariableElement;
        if (ElementUtils.isStatic(variableElement)) {
            // Static fields can never be @ReceiverDependantAssignable!
            return false;
        }
        return !isAssignableField(variableElement) && !isFinalField(variableElement);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement element = TreeUtils.elementFromDeclaration(node);
        if (element != null && element.getKind() == ElementKind.FIELD) {
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(element);
            if (type.hasAnnotation(PolyMutable.class)) {
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
        if (!(type.hasAnnotation(atypeFactory.IMMUTABLE) || type.hasAnnotation(atypeFactory.MUTABLE) ||
        type.hasAnnotation(atypeFactory.RECEIVERDEPENDANTMUTABLE) || type.hasAnnotation(atypeFactory.POLYMUTABLE))) {
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
            if (!atypeFactory.getTypeHierarchy().isSubtype(subClassConstructorReturnType, superClassConstructorReturnType, atypeFactory.READONLY)) {
                checker.report(
                        Result.failure(
                                "subclass.constructor.invalid", subClassConstructorReturnType, superClassConstructorReturnType), node);
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
            if (!(constructorReturnType.hasAnnotation(Immutable.class) || constructorReturnType.hasAnnotation(ReceiverDependantMutable.class))) {
                return;
            }
        }
        super.checkFieldsInitialized(blockNode, staticFields, store, receiverAnnotations);
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(atypeFactory.getQualifierHierarchy().getBottomAnnotation(atypeFactory.BOTTOM));
        result.add(atypeFactory.COMMITED);
        return result;
    }

    @Override
    protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(atypeFactory.getQualifierHierarchy().getTopAnnotation(atypeFactory.READONLY));
        result.add(atypeFactory.COMMITED);
        return result;
    }
}
