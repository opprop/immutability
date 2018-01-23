package pico.inference;

import checkers.inference.InferenceChecker;
import checkers.inference.InferenceMain;
import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import checkers.inference.SlotManager;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.EqualityConstraint;
import checkers.inference.model.InequalityConstraint;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import pico.typecheck.PICOTypeUtil;
import qual.Assignable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

/**
 * Generate constraints based on the PICO constraint-based type rules in infer mode. Has typecheck
 * and infer mode. In typecheck mode, has the exact same behaviour as PICOVisitor.
 */
public class PICOInferenceVisitor extends InferenceVisitor<PICOInferenceChecker, BaseAnnotatedTypeFactory> {

    protected final boolean infer;

    public PICOInferenceVisitor(PICOInferenceChecker checker, InferenceChecker ichecker, BaseAnnotatedTypeFactory factory, boolean infer) {
        super(checker, ichecker, factory, infer);
        this.infer = infer;
    }

    @Override
    protected InferenceValidator createTypeValidator() {
        return new PICOInferenceValidator(checker, this, atypeFactory);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        if (infer) {
            mainIsNot(declarationType, READONLY, "type.invalid", tree);
            addMutableImmutableIncompatibleConstraints(declarationType, useType);
            return true;
        } else {
            AnnotationMirror declared = declarationType.getAnnotationInHierarchy(READONLY);
            if (AnnotationUtils.areSame(declared, RECEIVER_DEPENDANT_MUTABLE)) {
                return true;
            }
            assert AnnotationUtils.areSame(declared, MUTABLE) ||
                    AnnotationUtils.areSame(declared, IMMUTABLE);

            AnnotationMirror used = useType.getAnnotationInHierarchy(READONLY);
            if (AnnotationUtils.areSame(declared, MUTABLE) && !AnnotationUtils.areSame(used, IMMUTABLE)) {
                return true;
            }

            if (AnnotationUtils.areSame(declared, IMMUTABLE) && !AnnotationUtils.areSame(used, MUTABLE)) {
                return true;
            }

            return false;
        }
    }

    private void addMutableImmutableIncompatibleConstraints(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType) {
        ConstraintManager constraintManager = InferenceMain.getInstance().getConstraintManager();
        SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        Slot declSlot = slotManager.getVariableSlot(declarationType);
        Slot useSlot = slotManager.getVariableSlot(useType);
        Slot mutable = slotManager.getSlot(MUTABLE);
        Slot immutable = slotManager.getSlot(IMMUTABLE);
        // declType == @Mutable -> useType != @Immutable
        EqualityConstraint equalityConstraint = constraintManager.createEqualityConstraint(declSlot, mutable);
        InequalityConstraint inequalityConstraint = constraintManager.createInequalityConstraint(useSlot, immutable);
        constraintManager.addImplicationConstraint(Arrays.asList(equalityConstraint), inequalityConstraint);
        // declType == @Immutable -> useType != @Mutable
        equalityConstraint = constraintManager.createEqualityConstraint(declSlot, immutable);
        inequalityConstraint = constraintManager.createInequalityConstraint(useSlot, mutable);
        constraintManager.addImplicationConstraint(Arrays.asList(equalityConstraint), inequalityConstraint);
    }

    @Override
    public boolean validateTypeOf(Tree tree) {
        AnnotatedTypeMirror type;
        // It's quite annoying that there is no TypeTree
        switch (tree.getKind()) {
            case PRIMITIVE_TYPE:
            case PARAMETERIZED_TYPE:
            case TYPE_PARAMETER:
            case ARRAY_TYPE:
            case UNBOUNDED_WILDCARD:
            case EXTENDS_WILDCARD:
            case SUPER_WILDCARD:
            case ANNOTATED_TYPE:
                type = atypeFactory.getAnnotatedTypeFromTypeTree(tree);
                break;
            case METHOD:
                type = atypeFactory.getMethodReturnType((MethodTree) tree);
                if (type == null ||
                        type.getKind() == TypeKind.VOID) {
                    // Nothing to do for void methods.
                    // Note that for a constructor the AnnotatedExecutableType does
                    // not use void as return type.
                    return true;
                }
                break;
            default:
                type = atypeFactory.getAnnotatedType(tree);
        }

        return validateType(tree, type);
    }

    // TODO This might not be correct for infer mode. Maybe returning as it is
    @Override
    public boolean validateType(Tree tree, AnnotatedTypeMirror type) {
        // basic consistency checks
        if (!AnnotatedTypes.isValidType(atypeFactory.getQualifierHierarchy(), type)) {
            if (!infer) {
                checker.report(
                        Result.failure("type.invalid", type.getAnnotations(), type.toString()), tree);
                return false;
            }
        }

        if (!typeValidator.isValid(type, tree)) {
            if (!infer) {
                return false;
            }
        }
        // The initial purpose of always returning true in validateTypeOf in inference mode
        // might be that inference we want to generate constraints over all the ast location,
        // but not like in typechecking mode, if something is not valid, we abort checking the
        // remaining parts that are based on the invalid type. For example, in assignment, if
        // rhs is not valid, we don't check the validity of assignment. But in inference,
        // we always generate constraints on all places and let solver to decide if there is
        // solution or not. This might be the reason why we have a always true if statement and
        // validity check always returns true.
        return true;
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType invocation, AnnotatedExecutableType constructor, NewClassTree newClassTree) {
        if (infer) {
            AnnotationMirror constructorReturn = extractVarAnnot(constructor.getReturnType());
            mainIsSubtype(invocation, constructorReturn, "constructor.invocation.invalid", newClassTree);
        } else {
            AnnotatedDeclaredType returnType = (AnnotatedDeclaredType) constructor.getReturnType();
            if (!atypeFactory.getTypeHierarchy().isSubtype(invocation, returnType)) {
                checker.report(Result.failure(
                        "constructor.invocation.invalid", invocation, returnType), newClassTree);
                return false;
            }
        }
        return super.checkConstructorInvocation(invocation, constructor, newClassTree);
    }

    private AnnotationMirror extractVarAnnot(final AnnotatedTypeMirror atm) {
        assert infer;
        final SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        return slotManager.getAnnotation(slotManager.getVariableSlot(atm));
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(node);

        MethodTree methodTree = (MethodTree) node;
        ExecutableElement element = TreeUtils.elementFromDeclaration(methodTree);
        TypeElement enclosingTypeElement = ElementUtils.enclosingClass(element);
        AnnotatedDeclaredType boundATM = atypeFactory.getAnnotatedType(enclosingTypeElement);

        if (TreeUtils.isConstructor(node)) {
            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) executableType.getReturnType();
            if (infer) {
                // Constructor return cannot be @Readonly
                mainIsNot(constructorReturnType, READONLY, "constructor.return.invalid", node);
                ConstraintManager constraintManager = InferenceMain.getInstance().getConstraintManager();
                SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
                Slot boundSlot = slotManager.getVariableSlot(boundATM);
                Slot consRetSlot = slotManager.getVariableSlot(constructorReturnType);
                Slot rdmSlot = slotManager.getSlot(RECEIVER_DEPENDANT_MUTABLE);
                InequalityConstraint inequalityConstraint = constraintManager.createInequalityConstraint(boundSlot, rdmSlot);
                SubtypeConstraint subtypeConstraint = constraintManager.createSubtypeConstraint(consRetSlot, boundSlot);
                // bound != @ReceiverDependantMutable -> consRet <: bound
                constraintManager.addImplicationConstraint(Arrays.asList(inequalityConstraint), subtypeConstraint);
            } else {
                // Doesn't check anonymous constructor case
                if (TreeUtils.isAnonymousConstructor(node)) {
                    return super.visitMethod(node, p);
                }
                if (constructorReturnType.hasAnnotation(READONLY)) {
                    checker.report(Result.failure("constructor.return.invalid", constructorReturnType), node);
                    return super.visitMethod(node, p);
                }
                if (boundATM.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                    // Any one of @Mutable, @ReceiverDependantMutable and @Immutable are allowed to be constructor
                    // return type if the class bound is @ReceiverDependantMutable.
                    return super.visitMethod(node, p);
                }
                if (!atypeFactory.getTypeHierarchy().isSubtype(constructorReturnType, boundATM)) {
                    checker.report(Result.failure("constructor.return.incompatible"), node);
                }
            }
        } else {
            AnnotatedDeclaredType declaredReceiverType = executableType.getReceiverType();
            if (declaredReceiverType != null) {
                if (infer) {
                    addMutableImmutableIncompatibleConstraints(boundATM, declaredReceiverType);
                } else {
                    AnnotationMirror boundAnnotation = PICOTypeUtil.getBoundAnnotationOnTypeDeclaration(enclosingTypeElement, atypeFactory);
                    AnnotationMirror declaredReceiverAnnotation = declaredReceiverType.getAnnotationInHierarchy(READONLY);
                    if (boundAnnotation != null
                            && !AnnotationUtils.areSame(boundAnnotation, RECEIVER_DEPENDANT_MUTABLE)// clone() method doesn't warn
                            && !atypeFactory.getQualifierHierarchy().isSubtype(declaredReceiverAnnotation, boundAnnotation)
                            // Below three are allowed on declared receiver types of instance methods in either @Mutable class or @Immutable class
                            && !AnnotationUtils.areSame(declaredReceiverAnnotation, READONLY)
                            && !AnnotationUtils.areSame(declaredReceiverAnnotation, RECEIVER_DEPENDANT_MUTABLE)) {
                        checker.report(Result.failure("method.receiver.incompatible", declaredReceiverType), node);
                    }
                }
            }
        }

        flexibleOverrideChecker(node);

        // TODO Object identity check
        return super.visitMethod(node, p);
    }

    // TODO Completely copied from PICOVisitor
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
            if (infer) {
                ((PICOInferenceAnnotatedTypeFactory)atypeFactory).viewpointAdaptMethod(pair.getValue(), enclosingType, overriddenMethod);
            } else {
                ((PICOInferenceRealTypeFactory)atypeFactory).viewpointAdaptMethod(pair.getValue(), enclosingType, overriddenMethod);
            }
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
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(variable);
        if (receiverType == null) {
            return super.visitAssignment(node, p);
        }
        if (PICOTypeUtil.isAssigningAssignableField(node, atypeFactory)) {
            checkAssignableField(node, variable, receiverType);
        } else if (isInitializingObject(node)) {
            checkInitializingObject(node, variable, receiverType);
        } else {
            checkMutableReceiverCase(node, variable, receiverType);
        }
        return super.visitAssignment(node, p);
    }

    private void checkAssignableField(AssignmentTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        if (infer) {
            // TODO PICOINF We just selected one way to break the combination of readonly receiver, assignable and rdm field
            // Does this make sense?
            mainIsNot(receiverType, READONLY, "illegal.field.write", node);
        } else {
            Element fieldElement = TreeUtils.elementFromUse(node);
            if (fieldElement != null) {//TODO Can this bu null?
                AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(fieldElement);
                if (receiverType.hasAnnotation(READONLY) && fieldType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                    reportFieldOrArrayWriteError(node, variable, receiverType);
                }
            }
        }
    }

    private void checkInitializingObject(AssignmentTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        if (infer) {
            // Can be anything from mutable, immutable or receiverdependantmutable
            mainIsNot(receiverType, READONLY, "illegal.field.write", node);
        } else {
            if (receiverType.hasAnnotation(READONLY)) {
                reportFieldOrArrayWriteError(node, variable, receiverType);
            }
        }
    }

    private void checkMutableReceiverCase(AssignmentTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        if (infer) {
            mainIs(receiverType, MUTABLE, "illegal.field.write", node);
        } else {
            if (!receiverType.hasAnnotation(MUTABLE)) {
                reportFieldOrArrayWriteError(node, variable, receiverType);
            }
        }
    }

    // Completely copied from PICOVisitor
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

    /**
     * Determines if an assignment is initializaing an object.
     *
     * True if:
     * 1) Inside initialization block
     * 2) In constructor
     * 3) In instance method, declared receiver is @UnderInitialized
     *
     * @param node assignment tree that might be initializing an object
     * @return true if the assignment tree is initializing an object
     *
     * @see #hasUnderInitializationDeclaredReceiver(MethodTree)
     */
    private boolean isInitializingObject(AssignmentTree node) {
        Element element = TreeUtils.elementFromUse(node);
        // If the assignment is not field assignment, there is no possibility of initializing object.
        if (element == null || !element.getKind().isField()) return false;
        ExpressionTree variable = node.getVariable();
        TreePath treePath = atypeFactory.getPath(node);
        if (treePath == null) return false;

        if (TreeUtils.enclosingTopLevelBlock(treePath) != null) {
            // In the initialization block => always allow assigning fields!
            return true;
        }

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(treePath);
        // No possibility of initialiazing object if the assignment is not within constructor or method(both MethodTree)
        if (enclosingMethod == null) return false;
        // At this point, we already know that this assignment is field assignment within a method
        if (TreeUtils.isConstructor(enclosingMethod) || hasUnderInitializationDeclaredReceiver(enclosingMethod)) {
            ExpressionTree receiverTree = TreeUtils.getReceiverTree(variable);
            if (receiverTree == null) {
                // Field access with implicit "this" receiver => Must be initializing object
                return true;
            } else {
                // Explicit receiver is "this", not other ordinary references or transitive chaining reference => Initializing object
                return receiverTree.toString().contentEquals("this");
            }
        } else {
            return false;
        }
    }

    private boolean hasUnderInitializationDeclaredReceiver(MethodTree mt) {
        // If there is not explicit "this" parameter or if there is not annotation on "this" parameter,
        // the method is not annotated with @UnderInitialization
        if (mt.getReceiverParameter() == null ||
                mt.getReceiverParameter().getModifiers().getAnnotations().isEmpty()) {
            return false;
        }
        Iterator<? extends AnnotationTree> iterator = mt.getReceiverParameter().getModifiers().getAnnotations().iterator();
        while (iterator.hasNext()) {
             if (iterator.next().getAnnotationType().toString().contains("UnderInitialization")) {
                 return true;
             }
        }
        return false;
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
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
        if (infer) {
            // Ensure only @Mutable/@Immutable/@ReceiverDependantMutable are inferred on new instance creation
            mainIsNoneOf(type, new AnnotationMirror[]{READONLY}, "pico.new.invalid", node);
        } else {
            if (type.hasAnnotation(READONLY)) {
                checker.report(Result.failure("pico.new.invalid", type), node);
            }
        }
    }

    /**This is really copied from PICOVisitor#visitMethodInvocation(MethodInvocationTree).*/
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

    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method, MethodInvocationTree node) {
        if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            AnnotatedTypeMirror subClassConstructorReturnType = atypeFactory.getReceiverType(node);
            AnnotatedTypeMirror superClassConstructorReturnType = method.getReturnType();
            // In infer mode, InferenceQualifierHierarchy that is internally used should generate subtype constraint between the
            // below two types
            if (!atypeFactory.getTypeHierarchy().isSubtype(subClassConstructorReturnType, superClassConstructorReturnType)) {
                if (infer) {
                    // Usually the subtyping check returns true. If not, that means subtype constraint doesn't hold between two
                    // ConstantSlots. Then this unsatisfiable constraint should be captured by ConstraintManager. So we don't report
                    // duplicate error message here.
                } else {
                    checker.report(
                            Result.failure(
                                    "subclass.constructor.invalid", subClassConstructorReturnType, superClassConstructorReturnType), node);
                }
            }
        }
        super.checkMethodInvocability(method, node);
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        if (infer) {
            result.add(createEquivalentVarAnnotOfRealQualifier(BOTTOM));
        } else {
            result.add(atypeFactory.getQualifierHierarchy().getBottomAnnotation(BOTTOM));
        }
        return result;
    }

    @Override
    protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        if (infer) {
            result.add(createEquivalentVarAnnotOfRealQualifier(READONLY));
        } else {
            result.add(atypeFactory.getQualifierHierarchy().getTopAnnotation(READONLY));
        }
        return result;
    }

    private AnnotationMirror createEquivalentVarAnnotOfRealQualifier(final AnnotationMirror am) {
        assert infer;
        final SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        ConstantSlot constantSlot = slotManager.createConstantSlot(am);
        return slotManager.getAnnotation(constantSlot);
    }
}
