package pico.inference;

import static org.checkerframework.javacutil.TreeUtils.elementFromTree;
import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
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

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.AnnotationTree;
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
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import checkers.inference.InferenceChecker;
import checkers.inference.InferenceMain;
import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import checkers.inference.SlotManager;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.EqualityConstraint;
import checkers.inference.model.InequalityConstraint;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import org.checkerframework.javacutil.TypesUtils;
import pico.typecheck.PICOAnnotatedTypeFactory;
import pico.typecheck.PICOTypeUtil;

/**
 * Generate constraints based on the PICO constraint-based type rules in infer mode. Has typecheck
 * and infer mode. In typecheck mode, has the exact same behaviour as PICOVisitor.
 */
public class PICOInferenceVisitor extends InferenceVisitor<PICOInferenceChecker, BaseAnnotatedTypeFactory> {

    public PICOInferenceVisitor(PICOInferenceChecker checker, InferenceChecker ichecker, BaseAnnotatedTypeFactory factory, boolean infer) {
        super(checker, ichecker, factory, infer);
    }

    @Override
    protected InferenceValidator createTypeValidator() {
        return new PICOInferenceValidator(checker, this, atypeFactory);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        return isAdaptedSubtype(useType, declarationType);

    }

    /**
     * constraint: lhs |> rhs <: lhs
     * equal to decl:immutable => use:immutable || decl:mutable => use:mutable
     * @param lhs left value of adaption, typically use
     * @param rhs right value of adaption, typically declaration
     * @return true if holds, always true during inference
     */
    private boolean isAdaptedSubtype(AnnotatedDeclaredType lhs, AnnotatedDeclaredType rhs) {
        ExtendedViewpointAdapter vpa = ((PublicViewpointAdapter)atypeFactory).getViewpointAdapter();
        AnnotatedTypeMirror adapted = vpa.rawCombineAnnotationWithType(lhs.getAnnotationInHierarchy(READONLY),
                rhs);
        return mainIsSubtype(adapted, lhs.getAnnotationInHierarchy(READONLY));
    }

    private void addMutableImmutableRdmIncompatibleConstraints(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType) {
        final ConstraintManager constraintManager = InferenceMain.getInstance().getConstraintManager();
        final SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        Slot declSlot = slotManager.getVariableSlot(declarationType);
        Slot useSlot = slotManager.getVariableSlot(useType);
        Slot mutable = slotManager.getSlot(MUTABLE);
        Slot immutable = slotManager.getSlot(IMMUTABLE);
        Slot rdm = slotManager.getSlot(RECEIVER_DEPENDANT_MUTABLE);
        // declType == @Mutable -> useType != @Immutable
        Constraint isMutable = constraintManager.createEqualityConstraint(declSlot, mutable);
        Constraint notImmutable = constraintManager.createInequalityConstraint(useSlot, immutable);
        constraintManager.addImplicationConstraint(Arrays.asList(isMutable), notImmutable);
        // declType == @Mutable -> useType != @ReceiverDependantMutable
        Constraint notRDM = constraintManager.createInequalityConstraint(useSlot, rdm);
        constraintManager.addImplicationConstraint(Arrays.asList(isMutable), notRDM);
        // declType == @Immutable -> useType != @Mutable
        Constraint isImmutable = constraintManager.createEqualityConstraint(declSlot, immutable);
        Constraint notMutable = constraintManager.createInequalityConstraint(useSlot, mutable);
        constraintManager.addImplicationConstraint(Arrays.asList(isImmutable), notMutable);
        // declType == @Immutable -> useType != @ReceiverDependantMutable
        constraintManager.addImplicationConstraint(Arrays.asList(isImmutable), notRDM);
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
        // TODO extends clause kind = IDENTIFIER. Add case here / override getAnnotatedType / annotator?
    }

    // TODO This might not be correct for infer mode. Maybe returning as it is
    @Override
    public boolean validateType(Tree tree, AnnotatedTypeMirror type) {

        if (!typeValidator.isValid(type, tree)) {  // in inference, isValid never return false
                return false;
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
    protected void checkConstructorInvocation(AnnotatedDeclaredType invocation, AnnotatedExecutableType constructor, NewClassTree newClassTree) {
        AnnotationMirror constructorReturn = extractVarAnnot(constructor.getReturnType());
        mainIsSubtype(invocation, constructorReturn, "constructor.invocation.invalid", newClassTree);

        super.checkConstructorInvocation(invocation, constructor, newClassTree);
    }

    private AnnotationMirror extractVarAnnot(final AnnotatedTypeMirror atm) {
        assert infer;
        final SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        return slotManager.getAnnotation(slotManager.getVariableSlot(atm));
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement element = TreeUtils.elementFromDeclaration(node);
        if (checker.hasOption("optimalSolution") && element != null
                && element.getKind() == ElementKind.FIELD && !ElementUtils.isStatic(element)) {
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(element);
            // Recursively prefer to be rdm and immutable
            addDeepPreference(type, RECEIVER_DEPENDANT_MUTABLE, 3, node);
            addDeepPreference(type, IMMUTABLE, 3, node);
        }
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(node);
        AnnotatedDeclaredType bound = PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(node, atypeFactory);

        if (TreeUtils.isConstructor(node)) {
            // Doesn't check anonymous constructor case
            if (TreeUtils.isAnonymousConstructor(node)) {
                return super.visitMethod(node, p);
            }

            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) executableType.getReturnType();
            // Constructor return cannot be @Readonly
            mainIsNot(constructorReturnType, READONLY, "constructor.return.invalid", node);

            if (infer) {
                ConstraintManager constraintManager = InferenceMain.getInstance().getConstraintManager();
                SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
                Slot boundSlot = slotManager.getVariableSlot(bound);
                Slot consRetSlot = slotManager.getVariableSlot(constructorReturnType);
                Slot rdmSlot = slotManager.getSlot(RECEIVER_DEPENDANT_MUTABLE);
                Constraint inequalityConstraint = constraintManager.createInequalityConstraint(boundSlot, rdmSlot);
                Constraint subtypeConstraint = constraintManager.createSubtypeConstraint(consRetSlot, boundSlot);
                // bound != @ReceiverDependantMutable -> consRet <: bound
                constraintManager.addImplicationConstraint(Arrays.asList(inequalityConstraint), subtypeConstraint);
                // TODO Add typecheck for this?
            }

        } else {
            // Additional logic compared to PICOVisitor to prefer declared receiver and parameters
            // tp be @Readonly in inference results.
            AnnotatedDeclaredType declaredReceiverType = executableType.getReceiverType();
            if (checker.hasOption("optimalSolution")) {
                if (declaredReceiverType != null) {
                    // Prefer declared receiver to be @Readonly
                    addDeepPreference(declaredReceiverType, READONLY, 1, node);
                }
                // Prefer all parameters to be @Readonly
                for (AnnotatedTypeMirror ptype : executableType.getParameterTypes()) {
                    addDeepPreference(ptype, READONLY, 1, node);
                }
            }
            // Above is additional preference logic
            if (declaredReceiverType != null) {
                assert bound != null;
                if (!isAdaptedSubtype(declaredReceiverType, bound)){
                    checker.report(Result.failure("method.receiver.incompatible", declaredReceiverType), node);
                }
            }
        }

        flexibleOverrideChecker(node);

        // TODO Object identity check
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
        if (infer && PICOTypeUtil.isEnclosedByAnonymousClass(node, atypeFactory)) {
            // Specially handle bound of anonymous type element in inference mode, as Inference-
            // TreeAnnotator doesn't support getting bound of anonymous class bound type
            enclosingType = PICOTypeUtil.getBoundOfEnclosingAnonymousClass(node, atypeFactory);
        }

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

    protected void checkTypecastSafety(TypeCastTree node, Void p) {
        if (!checker.getLintOption("cast:unsafe", true)) {
            return;
        }
        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(node);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());

        // We cannot do a simple test of casting, as isSubtypeOf requires
        // the input types to be subtypes according to Java
        if (!isTypeCastSafe(castType, exprType, node)) {
            // This is only warning message, so even though enterred this line, it doesn't cause PICOInfer to exit.
            checker.report(
                    Result.warning("cast.unsafe", exprType.toString(true), castType.toString(true)),
                    node);
        }
    }

    /**
     * PICO adapted method of checking typecast safety.
     *
     * In inference mode, to allow more programs to be inferred with results, let users to select type casting
     * strategy. Default is "comparablecast" - if the cast type is compatible with expression type, then it's ok.
     * In typechecking mode, PICO still warns if there is any potential unsafe casts, just to make programmer
     * notice them.
     *
     * @param castType type of cast/target
     * @param exprType type of original expression being casted
     * @param node {@link TypeCastTree} on which typecasting safety check happens
     * @return true if type casting is safe.
     *
     * @see {@link #isCompatibleCastInInfer(AnnotatedTypeMirror, AnnotatedTypeMirror, TypeCastTree)}
     */
    private boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType, TypeCastTree node) {
        if (infer) {
            return isCompatibleCastInInfer(castType, exprType, node);
        } else {
            // Typechecking side standard implementation - warns about downcasting
            return super.isTypeCastSafe(castType, exprType);
        }
    }

    /**
     * Method to determine if typecasting is safe in inference and generate constraints if necessary according to selected
     * strategy.
     *
     * In order to deal with different real world cases regarding to type cast, PICOInfer has three type casting strategies:
     * <p>
     * <ol>
     *  <li>upcast</li> Inferred result must satisfy {@code exprType <: castType}
     *  <li>anycast</li> In inferred result, {@code exprType} and {@code castType} can be any relation, e.g. subtype, incomparable.
     *  <li>comparablecast</li> Default strategy. In inferred result, {@code exprType <-> castType}, meaning they are comparable
     * </ol>
     *
     * <p>
     * For cases where there is at least one VariableSlot(to be inferred solution) between {@code castType} and {@code exprType},
     * this method always returns true, and generate corresponding {@link checkers.inference.model.Constraint} of the selected
     * strategy. For cases where two of the types are both Constants, there is no need of generating {@code Constraint}, so
     * no Constraints are generated for this case.
     * <p>
     * But one thing to note is that: the above three strategies only apply to solutions that PICOInfer will give. If there
     * are already existing annotations on both {@code castType} and {@code exprType}, even though relation between them may
     * not hold under the selected strategy, PICOInfer will issue a warning about it and continue execution, instead of giving
     * unsatisfiable error and exit the inference. This is primarily because casting is always a loophole in static type system,
     * and we can't say that all casts are errors.
     *
     * @param castType type to be casted into
     * @param exprType type of original expression that's going to be casted
     * @param node tree where type cast happens
     * @return true if type casting is safe
     */
    private boolean isCompatibleCastInInfer(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType, TypeCastTree node) {
        assert infer;

        if (checker.hasOption("upcast")) {
            // Upcast strategy - generate standard subtype constraint: exprType <: castType most often.
            return super.isTypeCastSafe(castType, exprType);
        } else if (checker.hasOption("anycast")) {
            // Anycast strategy - don't generate any constraint and any existing cast is seen as valid.
            return true;
        } else {
            // Default strategy - comparablecast
            final QualifierHierarchy qualHierarchy = InferenceMain.getInstance().getRealTypeFactory().getQualifierHierarchy();
            final SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
            final Slot castSlot = slotManager.getVariableSlot(castType);
            final Slot exprSlot = slotManager.getVariableSlot(exprType);

            if (castSlot instanceof ConstantSlot && exprSlot instanceof ConstantSlot) {
                ConstantSlot castCSSlot = (ConstantSlot) castSlot;
                ConstantSlot exprCSSlot = (ConstantSlot) exprSlot;
                // Special handling for case with two ConstantSlots: even though they may not be comparable,
                // but to infer more program, let this case fall back to "anycast" silently and continue
                // inference.
                return qualHierarchy.isSubtype(castCSSlot.getValue(), exprCSSlot.getValue())
                		|| qualHierarchy.isSubtype(exprCSSlot.getValue(), castCSSlot.getValue());
            } else {
                // But if there is at least on VariableSlot, PICOInfer guarantees that solutions don't include
                // incomparable casts.
                areComparable(castType, exprType, "flexible.cast.unsafe", node);
                return true;
            }
        }
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        ExpressionTree variable = node.getVariable();
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



    private void checkMutation(ExpressionTree node, ExpressionTree variable) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(variable);
        if(receiverType != null) {
            if (PICOTypeUtil.isAssigningAssignableField(node, atypeFactory)){
                checkAssignableField(node, variable, receiverType);
            } else if (isInitializingObject(node)) {
                checkInitializingObject(node, variable, receiverType);
            } else {
                checkMutableReceiverCase(node, variable, receiverType);
            }
        }
    }

    private void checkAssignableField(ExpressionTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        Element fieldElement = TreeUtils.elementFromUse(node);
        if (fieldElement != null) {//TODO Can this bu null?
            AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(fieldElement);
            assert  fieldType != null;
            if (infer) {
                // Break the combination of readonly receiver + rdm assignable field
                ConstraintManager constraintManager = InferenceMain.getInstance().getConstraintManager();
                SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
                Slot receiverSlot = slotManager.getVariableSlot(receiverType);
                Slot fieldSlot = slotManager.getVariableSlot(fieldType);
                Slot readonly = slotManager.getSlot(READONLY);
                Slot receiver_dependant_mutable = slotManager.getSlot(RECEIVER_DEPENDANT_MUTABLE);
                Constraint receiverReadOnly = constraintManager.createEqualityConstraint(receiverSlot, readonly);
                Constraint fieldNotRDM = constraintManager.createInequalityConstraint(fieldSlot, receiver_dependant_mutable);
                //  receiver = READONLY
                constraintManager.addImplicationConstraint(Arrays.asList(receiverReadOnly), fieldNotRDM);
            } else {
                if (receiverType.hasAnnotation(READONLY) && fieldType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                    reportFieldOrArrayWriteError(node, variable, receiverType);
                }
            }
        }
    }

    private void checkInitializingObject(ExpressionTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        // TODO rm infer after mainIsNot returns bool
        if (infer) {
            // Can be anything from mutable, immutable or receiverdependantmutable
            mainIsNot(receiverType, READONLY, "illegal.field.write", node);
        } else {
            if (receiverType.hasAnnotation(READONLY)) {
                reportFieldOrArrayWriteError(node, variable, receiverType);
            }
        }
    }

    private void checkMutableReceiverCase(ExpressionTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        // TODO rm infer after mainIs returns bool
        if (infer) {
            mainIs(receiverType, MUTABLE, "illegal.field.write", node);
        } else {
            if (!receiverType.hasAnnotation(MUTABLE)) {
                reportFieldOrArrayWriteError(node, variable, receiverType);
            }
        }
    }

    // Completely copied from PICOVisitor
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

    /**
     * Determines if an assignment is initializaing an object.
     *
     * True if:
     * 1) Inside initialization block
     * 2) In constructor
     * 3) In instance method, declared receiver is @UnderInitialized
     *
     * @param variable assignment tree that might be initializing an object
     * @return true if the assignment tree is initializing an object
     *
     * @see #hasUnderInitializationDeclaredReceiver(MethodTree)
     */
    private boolean isInitializingObject(ExpressionTree variable) {
        Element element = TreeUtils.elementFromUse(variable);
        // If the assignment is not field assignment, there is no possibility of initializing object.
        if (element == null || !element.getKind().isField()) return false;
        TreePath treePath = atypeFactory.getPath(variable);
        if (treePath == null) return false;

        if (TreeUtils.enclosingTopLevelBlock(treePath) != null) {
            // In the initialization block => always allow assigning fields!
            return true;
        }

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(treePath);
        // No possibility of initializing object if the assignment is not within constructor or method(both MethodTree)
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
        // Ensure only @Mutable/@Immutable/@ReceiverDependantMutable are inferred on new instance creation
        mainIsNoneOf(type, new AnnotationMirror[]{READONLY}, "pico.new.invalid", node);
    }

    // Completely copied from PICOVisitor
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

    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method, MethodInvocationTree node) {
        if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            AnnotatedTypeMirror subClassConstructorReturnType = atypeFactory.getReceiverType(node);
            AnnotatedTypeMirror superClassConstructorReturnType = method.getReturnType();
            // In infer mode, InferenceQualifierHierarchy that is internally used should generate subtype constraint between the
            // below two types GENERALLY(not always)
            if (!atypeFactory.getTypeHierarchy().isSubtype(subClassConstructorReturnType, superClassConstructorReturnType)) {
                // Usually the subtyping check returns true. If not, that means subtype constraint doesn't hold between two
                // ConstantSlots. Previously, InferenceQualifierHierarchy also generates subtype constraint in this case,
                // then this unsatisfiable constraint is captured by ConstraintManager and ConstraintManager early exits. But
                // now for two ConstantSlot case, no subtype constraint is generated any more. So we have to report the error
                // , otherwise it will cause inference result not typecheck
                checker.report(
                        Result.failure(
                                "super.invocation.invalid", subClassConstructorReturnType, superClassConstructorReturnType), node);
            }
        }
        super.checkMethodInvocability(method, node);
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        if (infer) {
            result.add(PICOTypeUtil.createEquivalentVarAnnotOfRealQualifier(BOTTOM));
        } else {
            result.add(atypeFactory.getQualifierHierarchy().getBottomAnnotation(BOTTOM));
        }
        return result;
    }

    @Override
    protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        if (infer) {
            result.add(PICOTypeUtil.createEquivalentVarAnnotOfRealQualifier(READONLY));
        } else {
            result.add(atypeFactory.getQualifierHierarchy().getTopAnnotation(READONLY));
        }
        return result;
    }

    @Override
    public void processClassTree(ClassTree node) {
        TypeElement typeElement = TreeUtils.elementFromDeclaration(node);
        // TODO Don't process anonymous class. I'm not even sure if whether processClassTree(ClassTree) is
        // called on anonymous class tree
        if (TypesUtils.isAnonymous(TreeUtils.typeOf(node))) {
            super.processClassTree(node);
            return;
        }

        AnnotatedDeclaredType bound = PICOTypeUtil.getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);

        mainIsNot(bound, READONLY, "class.bound.invalid", node);
        if (checker.hasOption("optimalSolution")) {
            addPreference(bound, RECEIVER_DEPENDANT_MUTABLE, 2);
            addPreference(bound, IMMUTABLE, 2);
        }

        if (!checkCompatabilityBetweenBoundAndSuperClassesBounds(node, typeElement, bound)) {
            return;
        }

        if (node.getExtendsClause() != null) {
            // compare extends type with its decl
            Tree extClause = node.getExtendsClause();
            AnnotatedTypeMirror ext = atypeFactory.getTypeOfExtendsImplements(extClause);
            AnnotatedTypeMirror extDecl = atypeFactory.getAnnotatedType(TreeUtils.elementFromTree(extClause));
            System.err.println("V: "+ext);
            assert ext instanceof AnnotatedDeclaredType;
            assert extDecl instanceof AnnotatedDeclaredType;


            if (!isAdaptedSubtype((AnnotatedDeclaredType) ext, (AnnotatedDeclaredType) extDecl)) {
                checker.report(
                        Result.failure(
                                "invalid.annotation.on.use", node.getExtendsClause()), node);
            }
        }


//        if (!checkCompatabilityBetweenBoundAndExtendsImplements(node, bound)) {
//            return;
//        }

        // Reach this point iff 1) bound annotation is one of mutable, rdm or immutable;
        // 2) bound is compatible with bounds on super types. Only then continue processing
        // the class tree
        super.processClassTree(node);
    }

    private boolean checkCompatabilityBetweenBoundAndSuperClassesBounds(ClassTree node, TypeElement typeElement, AnnotatedDeclaredType bound) {
        // Must have compatible bound annotation as the direct super types
        List<AnnotatedDeclaredType> superBounds = PICOTypeUtil.getBoundTypesOfDirectSuperTypes(typeElement, atypeFactory);
        for (AnnotatedDeclaredType superBound : superBounds) {
            if (infer) {
                addSameToMutableImmutableConstraints(superBound, bound);
            } else {
                // If annotation on super bound is @ReceiverDependantMutable, then any valid bound is permitted.
                if (superBound.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) continue;
                // super bound is either @Mutable or @Immutable. Must be the subtype of the corresponding super bound
                if (!atypeFactory.getQualifierHierarchy().isSubtype(
                        bound.getAnnotationInHierarchy(READONLY), superBound.getAnnotationInHierarchy(READONLY))) {
                    checker.report(Result.failure("subclass.bound.incompatible", bound, superBound), node);
                    return true;
                }
            }
        }
        return true;
    }

    private boolean checkCompatabilityBetweenBoundAndExtendsImplements(ClassTree node, AnnotatedDeclaredType bound) {
        if (infer) {
            atypeFactory.getAnnotatedType(node);
        }

        boolean hasSame;
        Tree ext = node.getExtendsClause();
        if (ext != null) {
            AnnotatedTypeMirror extendsType= atypeFactory.getAnnotatedType(ext);
            if (infer) {
                ((PICOInferenceAnnotatedTypeFactory) atypeFactory).getVariableAnnotator().visit(extendsType, ext);
                areEqual(bound, extendsType, "bound.extends.incompatible", node);
            } else {
                AnnotationMirror superAnnoOnUse = atypeFactory.fromTypeTree(ext).getAnnotationInHierarchy(READONLY);

                hasSame = bound.getAnnotations().size() == extendsType.getAnnotations().size()
                        && AnnotationUtils.areSame(extendsType.getAnnotationInHierarchy(READONLY),
                        bound.getAnnotationInHierarchy(READONLY));
                // TODO rdm defaulting should be prevented instead of suppressing error here
                boolean defaultedRdm = superAnnoOnUse == null &&
                        AnnotationUtils.areSame(extendsType.getAnnotationInHierarchy(READONLY), RECEIVER_DEPENDANT_MUTABLE);
                if (!defaultedRdm && !hasSame) {
                        checker.report(Result.failure("bound.extends.incompatible"), node);
                        return true;
                }
            }
        }

        List<? extends Tree> impls = node.getImplementsClause();
        if (impls != null) {
            for (Tree im : impls) {
                // TODO: why @ from stub does not present when using getAnnotatedType(Tree)?
                // hint: however getAnnotatedType(ExpressionTree) works perfectly.
                AnnotatedTypeMirror implementsType = atypeFactory.getAnnotatedType(elementFromTree(im));
                if (infer) {
                    ((PICOInferenceAnnotatedTypeFactory) atypeFactory).getVariableAnnotator().visit(implementsType, im);
                    areEqual(bound, implementsType, "bound.implements.incompatible", node);
                } else {
                    hasSame = bound.getAnnotations().size() == implementsType.getAnnotations().size()
                            && AnnotationUtils.areSame(implementsType.getAnnotationInHierarchy(READONLY),
                            bound.getAnnotationInHierarchy(READONLY));
                    if (!hasSame) {
                        // mutable and immutable could extends RDM. See Mier's thesis 4.8.3 (p.43)
                        if (!(bound.getAnnotations().size() == implementsType.getAnnotations().size()
                                && AnnotationUtils.areSame(implementsType.getAnnotationInHierarchy(READONLY),
                                RECEIVER_DEPENDANT_MUTABLE)
                                && (AnnotationUtils.areSame(bound.getAnnotationInHierarchy(READONLY),
                                MUTABLE)
                                || AnnotationUtils.areSame(bound.getAnnotationInHierarchy(READONLY),
                                IMMUTABLE)))) {
                            checker.report(Result.failure("bound.implements.incompatible"), node);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void addSameToMutableImmutableConstraints(AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType) {
        ConstraintManager constraintManager = InferenceMain.getInstance().getConstraintManager();
        SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        Slot declSlot = slotManager.getVariableSlot(declarationType);
        Slot useSlot = slotManager.getVariableSlot(useType);
        Slot mutable = slotManager.getSlot(MUTABLE);
        Slot immutable = slotManager.getSlot(IMMUTABLE);
        // declType == @Mutable -> useType == @Mutable
        Constraint equalityConstraintLHS = constraintManager.createEqualityConstraint(declSlot, mutable);
        Constraint equalityConstraintRHS = constraintManager.createEqualityConstraint(useSlot, mutable);
        constraintManager.addImplicationConstraint(Arrays.asList(equalityConstraintLHS), equalityConstraintRHS);
        // declType == @Immutable -> useType == @Immutable
        equalityConstraintLHS = constraintManager.createEqualityConstraint(declSlot, immutable);
        equalityConstraintRHS = constraintManager.createEqualityConstraint(useSlot, immutable);
        constraintManager.addImplicationConstraint(Arrays.asList(equalityConstraintLHS), equalityConstraintRHS);
    }

    /**
     * commonAssignmentCheck() method that adapts to PICOInfer.
     *
     * In inference mode, pass viewpoint adapted field type to enclosing class to lhs type. To avoid side effect,
     * instead of directly adapting field's type, copy that, and viewpoint adapt it as the type of field, so that
     * original field type still have the same VarAnnot, yet the lhs type is now a different combined VarAnnot.
     * @param varTree the AST node for the variable
     * @param valueExp the AST node for the value
     * @param errorKey the error message to use if the check fails (must be a
     */
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
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
                                         AnnotatedTypeMirror valueType, Tree valueTree,
                                                 String errorKey) {
        // TODO: WORKAROUND: anonymous class handling
        if (TypesUtils.isAnonymous(valueType.getUnderlyingType())) {
            AnnotatedTypeMirror newValueType = varType.deepCopy();
            newValueType.replaceAnnotation(valueType.getAnnotationInHierarchy(READONLY));
            valueType = newValueType;
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);

    }
}
