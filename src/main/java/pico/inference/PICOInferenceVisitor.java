package pico.inference;

import checkers.inference.InferenceChecker;
import checkers.inference.InferenceMain;
import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import checkers.inference.SlotManager;
import com.sun.source.tree.AnnotationTree;
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
import com.sun.source.util.TreePath;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
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
import qual.Assignable;
import qual.Immutable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.Iterator;
import java.util.List;

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
        // Doesn't generate subtype constraints between usedType and declarationType
        return true;
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
            AnnotationMirror constructorReturn = getVarAnnot(constructor.getReturnType());
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

    protected AnnotationMirror getVarAnnot(final AnnotatedTypeMirror atm) {
        final SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        return slotManager.getAnnotation(slotManager.getVariableSlot(atm));
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(node);
        boolean hasImmutableBoundAnnotation = hasImmutableAnnotationOnTypeDeclaration(node);
        if (TreeUtils.isConstructor(node)) {
            AnnotatedDeclaredType constructorReturnType = (AnnotatedDeclaredType) executableType.getReturnType();
            if (infer) {
                mainIsNot(constructorReturnType, READONLY, "constructor.return.invalid", node);
            } else {
                if (constructorReturnType.hasAnnotation(READONLY)) {
                    checker.report(Result.failure("constructor.return.invalid", constructorReturnType), node);
                }
            }

            if (hasImmutableBoundAnnotation) {
                if(infer) {
                    mainIsSubtype(constructorReturnType, IMMUTABLE, "immutable.class.constructor.invalid", node);
                } else {
                    AnnotationMirror constructorReturnAnnotation = constructorReturnType.getAnnotationInHierarchy(READONLY);
                    if(!atypeFactory.getQualifierHierarchy().isSubtype(constructorReturnAnnotation, IMMUTABLE)) {
                        checker.report(Result.failure("immutable.class.constructor.invalid"), node);
                    }
                }
            }
            /*Doesn't check constructor parameters if constructor return is immutable or receiverdependantmutable*/
        } else {
            AnnotatedDeclaredType declareReceiverType = executableType.getReceiverType();
            if (hasImmutableBoundAnnotation && declareReceiverType != null) {
                if (infer) {
                    mainIsNoneOf(declareReceiverType,
                            new AnnotationMirror[]{MUTABLE, RECEIVER_DEPENDANT_MUTABLE, BOTTOM},
                            "immutable.class.method.receiver.invalid", node.getReceiverParameter());
                } else {
                    if(!(declareReceiverType.hasAnnotation(READONLY) ||
                            declareReceiverType.hasAnnotation(IMMUTABLE)))
                        checker.report(Result.failure("immutable.class.method.receiver.invalid"), node.getReceiverParameter());
                }
            }
        }
        return super.visitMethod(node, p);
    }

    // Completely copied from PICOVisitor
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
        AnnotationMirror boundAnnotation = bound.getAnnotationInHierarchy(READONLY);
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
        // TODO INF-FR Constructor return type can correct constraints and solutions now, but we don't insert the result
        // back into constructor return type.
        // TODO INF-FR Right now, "this" parameter in initialization block doesn't have VarAnnot. No matter 2nd or 3rd
        // branch is executed, mainIsNot silently ignore the constraint and mainIs throws NPE in DefaultSlotManager#
        // getAnnotation() (the method I added)
        // Must be a field assignment or array write
        if (isAssigningAssignableField(node)) {
            checkAssignableField(node, variable, receiverType);
        } else if (isInitializingObject(node)) {
            checkInitializingObject(node, variable, receiverType);
        } else {
            checkOtherAssignmentCase(node, variable, receiverType);
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

    private void checkOtherAssignmentCase(AssignmentTree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
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

    private boolean isAssigningAssignableField(AssignmentTree node) {
        Element field = TreeUtils.elementFromUse(node);
        if (field == null) return false;
        return isAssignableField(field);

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
}
