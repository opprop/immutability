package pico.typecheck;

import checkers.inference.InferenceMain;
import checkers.inference.SlotManager;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.Slot;
import checkers.inference.util.InferenceUtil;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import qual.Assignable;
import qual.Immutable;
import qual.ObjectIdentityMethod;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

public class PICOTypeUtil {

    private static final Set<Tree.Kind> sideEffectingUnaryOperators;

    static {
        sideEffectingUnaryOperators = new HashSet<>();
        sideEffectingUnaryOperators.add(Tree.Kind.POSTFIX_INCREMENT);
        sideEffectingUnaryOperators.add(Tree.Kind.PREFIX_INCREMENT);
        sideEffectingUnaryOperators.add(Tree.Kind.POSTFIX_DECREMENT);
        sideEffectingUnaryOperators.add(Tree.Kind.PREFIX_DECREMENT);
    }

    private static boolean isInTypeKindsOfDefaultForOfImmutable(AnnotatedTypeMirror atm) {
        DefaultFor defaultFor = Immutable.class.getAnnotation(DefaultFor.class);
        assert defaultFor != null;
        for (TypeKind typeKind : defaultFor.typeKinds()) {
            if (typeKind.name().equals(atm.getKind().name())) return true;
        }
        return false;
    }

    private static boolean isInTypesOfDefaultForOfImmutable(AnnotatedTypeMirror atm) {
        if (!atm.getKind().name().equals(TypeKind.DECLARED.name())) {
            return false;
        }
        DefaultFor defaultFor = Immutable.class.getAnnotation(DefaultFor.class);
        assert defaultFor != null;
        Class<?>[] types = defaultFor.types();
        String fqn = TypesUtils.getQualifiedName((DeclaredType) atm.getUnderlyingType()).toString();
        for (Class<?> type : types) {
            if (type.getCanonicalName().contentEquals(fqn)) {
                return true;
            }
        }
        return false;
    }

    /**Method to determine if the underlying type is implicitly immutable. This method is consistent
     * with the types and typeNames that are in @ImplicitFor in the definition of @Immutable qualifier*/
    public static boolean isImplicitlyImmutableType(AnnotatedTypeMirror atm) {
        return isInTypeKindsOfDefaultForOfImmutable(atm)
                || isInTypesOfDefaultForOfImmutable(atm);
    }

    /**
     * Returns the bound of type declaration enclosing the node.
     *
     * If no annotation exists on type declaration, bound is defaulted to @Mutable instead of having empty annotations.
     *
     * This method simply gets/defaults annotation on bounds of classes, but
     * doesn't validate the correctness of the annotation. They are validated in {@link PICOVisitor#processClassTree(ClassTree)}
     * method.
     *
     * @param node tree whose enclosing type declaration's bound annotation is to be extracted
     * @param atypeFactory pico type factory
     * @return annotation on the bound of enclosing type declaration
     */
    public static AnnotatedDeclaredType getBoundTypeOfEnclosingTypeDeclaration(Tree node, AnnotatedTypeFactory atypeFactory) {
        TypeElement typeElement = null;
        if (node instanceof MethodTree) {
            MethodTree methodTree = (MethodTree) node;
            ExecutableElement element = TreeUtils.elementFromDeclaration(methodTree);
            typeElement = ElementUtils.enclosingClass(element);
        } else if(node instanceof VariableTree) {
            VariableTree variableTree = (VariableTree) node;
            VariableElement variableElement = TreeUtils.elementFromDeclaration(variableTree);
            assert variableElement!= null && variableElement.getKind().isField();
            typeElement = ElementUtils.enclosingClass(variableElement);
        }

        if (typeElement != null) {
            return getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
        }

        return null;
    }

    public static AnnotatedDeclaredType getBoundTypeOfEnclosingTypeDeclaration(Element element, AnnotatedTypeFactory atypeFactory) {
        TypeElement typeElement = ElementUtils.enclosingClass(element);
        if (typeElement != null) {
            return getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
        }
        return null;
    }

    public static List<AnnotatedDeclaredType> getBoundTypesOfDirectSuperTypes(TypeElement current, AnnotatedTypeFactory atypeFactory) {
        List<AnnotatedDeclaredType> boundsOfSupers = new ArrayList<>();
        TypeMirror supertypecls;
        try {
            supertypecls = current.getSuperclass();
        } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
            // Copied from ElementUtils#getSuperTypes(Elements, TypeElement)
            // Looking up a supertype failed. This sometimes happens
            // when transitive dependencies are not on the classpath.
            // As javac didn't complain, let's also not complain.
            // TODO: Use an expanded ErrorReporter to output a message.
            supertypecls = null;
        }

        if (supertypecls != null && !supertypecls.getKind().name().equals(TypeKind.NONE.name())) {
            TypeElement supercls = (TypeElement) ((DeclaredType) supertypecls).asElement();
            boundsOfSupers.add(getBoundTypeOfTypeDeclaration(supercls, atypeFactory));
        }

        for (TypeMirror supertypeitf : current.getInterfaces()) {
            TypeElement superitf = (TypeElement) ((DeclaredType) supertypeitf).asElement();
            boundsOfSupers.add(getBoundTypeOfTypeDeclaration(superitf, atypeFactory));
        }
        return boundsOfSupers;
    }

    public static AnnotatedDeclaredType getBoundTypeOfTypeDeclaration(ClassTree classTree, AnnotatedTypeFactory atypeFactory) {
        TypeElement typeElement = TreeUtils.elementFromDeclaration(classTree);
        return getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
    }

    public static AnnotatedDeclaredType getBoundTypeOfTypeDeclaration(TypeElement typeElement, AnnotatedTypeFactory atypeFactory) {
        // Reads bound annotation from source code or stub files
        // Implicitly immutable types have @Immutable in its bound
        // All other elements that are: not implicitly immutable types specified in definition of @Immutable qualifier;
        // Or has no bound annotation on its type element declaration either in source tree or stub file(jdk.astub) have
        // @Mutable in its bound
        return atypeFactory.getAnnotatedType(typeElement);

        // It's a bit strange that bound annotations on implicilty immutable types
        // are not specified in the stub file. For implicitly immutable types, having bounds in stub
        // file suppresses type cast warnings, because in base implementation, it checks cast type is whether
        // from element itself. If yes, no matter what the casted type is, the warning is suppressed, which is
        // also not wanted. BUT, they are applied @Immutable as their bounds CORRECTLY, because we have TypeAnnotator!

        // TODO This method doesn't have logic of handling anonymous class! We should implement it, maybe in different
        // places, at some time.
    }

    public static boolean isObjectIdentityMethod(MethodTree node,
                                                 AnnotatedTypeFactory annotatedTypeFactory) {
        Element element = TreeUtils.elementFromTree(node);
        return isObjectIdentityMethod((ExecutableElement) element, annotatedTypeFactory);

    }

    public static boolean isObjectIdentityMethod(ExecutableElement executableElement,
                                                 AnnotatedTypeFactory annotatedTypeFactory) {
        return hasObjectIdentityMethodDeclAnnotation(executableElement, annotatedTypeFactory) ||
                isMethodOrOverridingMethod(executableElement, "hashCode()", annotatedTypeFactory) ||
                isMethodOrOverridingMethod(executableElement, "equals(java.lang.Object)", annotatedTypeFactory);
    }

    private static boolean hasObjectIdentityMethodDeclAnnotation(ExecutableElement element,
                                                                AnnotatedTypeFactory annotatedTypeFactory) {
        return annotatedTypeFactory.getDeclAnnotation(element, ObjectIdentityMethod.class) != null;
    }

    /**Helper method to determine a method using method name*/
    public static boolean isMethodOrOverridingMethod(AnnotatedExecutableType methodType, String methodName, AnnotatedTypeFactory annotatedTypeFactory) {
        return isMethodOrOverridingMethod(methodType.getElement(), methodName, annotatedTypeFactory);
    }

    public static boolean isMethodOrOverridingMethod(ExecutableElement executableElement, String methodName, AnnotatedTypeFactory annotatedTypeFactory) {
        // Check if it is the target method
        if (executableElement.toString().contentEquals(methodName)) return true;
        // Check if it is overriding the target method
        // Because AnnotatedTypes.overriddenMethods returns all the methods overriden in the class hierarchy, we need to
        // iterate over the set to check if it's overriding corresponding methods specifically in java.lang.Object class
        Iterator<Map.Entry<AnnotatedDeclaredType, ExecutableElement>> overriddenMethods
                = AnnotatedTypes.overriddenMethods(annotatedTypeFactory.getElementUtils(), annotatedTypeFactory, executableElement)
                .entrySet().iterator();
        while (overriddenMethods.hasNext()) {
            if (overriddenMethods.next().getValue().toString().contentEquals(methodName)) {
                return true;
            }
        }
        return false;
    }

    public static void addDefaultForField(AnnotatedTypeFactory annotatedTypeFactory,
                                          AnnotatedTypeMirror annotatedTypeMirror, Element element) {
        if (element != null && element.getKind() == ElementKind.FIELD) {
           if (ElementUtils.isStatic(element)) {
               AnnotatedTypeMirror explicitATM = annotatedTypeFactory.fromElement(element);
               if (!explicitATM.isAnnotatedInHierarchy(READONLY)) {
                   if (!PICOTypeUtil.isImplicitlyImmutableType(explicitATM)) {
                       annotatedTypeMirror.replaceAnnotation(MUTABLE);
                   }
               }
           } else {
               AnnotatedTypeMirror explicitATM = annotatedTypeFactory.fromElement(element);
               if (!explicitATM.isAnnotatedInHierarchy(READONLY)) {
                   if (explicitATM instanceof AnnotatedDeclaredType) {
                       AnnotatedDeclaredType adt = (AnnotatedDeclaredType) explicitATM;
                       Element typeElement = adt.getUnderlyingType().asElement();
                       if (typeElement instanceof TypeElement) {
                           AnnotatedDeclaredType bound = getBoundTypeOfTypeDeclaration((TypeElement) typeElement, annotatedTypeFactory);
                           if (bound.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                               annotatedTypeMirror.replaceAnnotation(RECEIVER_DEPENDANT_MUTABLE);
                           }
                       }
                   } else if (explicitATM instanceof AnnotatedArrayType) {
                       // Also apply rdm to array main.
                       annotatedTypeMirror.replaceAnnotation(RECEIVER_DEPENDANT_MUTABLE);
                   }
               }
           }
        }
    }

    public static boolean isEnumOrEnumConstant(AnnotatedTypeMirror annotatedTypeMirror) {
        if (!(annotatedTypeMirror instanceof AnnotatedDeclaredType)) {
            return false;
        }
        Element element = ((AnnotatedDeclaredType)annotatedTypeMirror).getUnderlyingType().asElement();
        return element != null
                && (element.getKind() == ElementKind.ENUM_CONSTANT || element.getKind() == ElementKind.ENUM);

    }

    public static void applyImmutableToEnumAndEnumConstant(AnnotatedTypeMirror annotatedTypeMirror) {
        if (isEnumOrEnumConstant(annotatedTypeMirror)) {
            // I guess enum constant can't have explicit annotation, am I right?
            annotatedTypeMirror.addMissingAnnotations(Arrays.asList(IMMUTABLE));
        }
    }

    // Default annotation on type declaration to constructor return type if elt is constructor and doesn't have
    // explicit annotation(type is actually AnnotatedExecutableType of executable element - elt constructor)
    public static void defaultConstructorReturnToClassBound(AnnotatedTypeFactory annotatedTypeFactory,
                                                            Element elt, AnnotatedTypeMirror type) {
        if (elt.getKind() == ElementKind.CONSTRUCTOR && type instanceof AnnotatedExecutableType) {
            AnnotatedDeclaredType bound = PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(elt, annotatedTypeFactory);
            ((AnnotatedExecutableType) type).getReturnType().addMissingAnnotations(Arrays.asList(bound.getAnnotationInHierarchy(READONLY)));
        }
    }

    public static void applyConstant(AnnotatedTypeMirror type, AnnotationMirror am) {
        SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        ConstraintManager constraintManager = InferenceMain.getInstance().getConstraintManager();
        // Might be null. It's normal. In typechecking side, we use addMissingAnnotations(). Only if
        // there is existing annotation in code, then here is non-null. Otherwise, VariableAnnotator
        // hasn't come into the picture yet, so no VarAnnot exists here, which is normal.
        Slot shouldBeAppliedTo = slotManager.getVariableSlot(type);
        ConstantSlot constant = slotManager.createConstantSlot(am);
        if (shouldBeAppliedTo == null) {
            // Here, we are adding VarAnnot that represents @Immutable. There won't be solution for this ConstantSlot for this type,
            // so the inserted-back source code doesn't have explicit annotation @Immutable. But it is not wrong. It makes the code
            // cleaner by omitting implicit annotations. General principle is that for ConstantSlot, there won't be annotation inserted
            // back to the original source code, BUT this ConstantSlot(representing @Immutable) will be used for constraint generation
            // that affects the solutions for other VariableSlots
            type.addAnnotation(slotManager.getAnnotation(constant));// Insert Constant VarAnnot that represents @Immutable
            type.addAnnotation(am);// Insert real @Immutable. This should be removed if INF-FR only uses VarAnnot
        } else {
            constraintManager.addEqualityConstraint(shouldBeAppliedTo, constant);
        }
    }

    /**Check if a field is final or not.*/
    public static boolean isFinalField(Element variableElement) {
        assert variableElement instanceof VariableElement;
        return ElementUtils.isFinal(variableElement);
    }

    /**Check if a field is assignable or not.*/
    public static boolean isAssignableField(Element variableElement, AnnotationProvider provider) {
        if (!(variableElement instanceof VariableElement)) {
            return false;
        }
        boolean hasExplicitAssignableAnnotation = provider.getDeclAnnotation(variableElement, Assignable.class) != null;
        if (!ElementUtils.isStatic(variableElement)) {
            // Instance fields must have explicit @Assignable annotation to be assignable
            return hasExplicitAssignableAnnotation;
        } else {
            // If there is explicit @Assignable annotation on static fields, then it's assignable; If there isn't,
            // and the static field is not final, we treat it as if it's assignable field.
            return hasExplicitAssignableAnnotation || !isFinalField(variableElement);
        }
    }

    /**Check if a field is @ReceiverDependantAssignable. Static fields always returns false.*/
    public static boolean isReceiverDependantAssignable(Element variableElement, AnnotationProvider provider) {
        assert variableElement instanceof VariableElement;
        if (ElementUtils.isStatic(variableElement)) {
            // Static fields can never be @ReceiverDependantAssignable!
            return false;
        }
        return !isAssignableField(variableElement, provider) && !isFinalField(variableElement);
    }

    public static boolean hasOneAndOnlyOneAssignabilityQualifier(VariableElement field, AnnotationProvider provider) {
        boolean valid = false;
        if (isAssignableField(field, provider) && !isFinalField(field) && !isReceiverDependantAssignable(field, provider)) {
            valid = true;
        } else if (!isAssignableField(field, provider) && isFinalField(field) && !isReceiverDependantAssignable(field, provider)) {
            valid = true;
        } else if (!isAssignableField(field, provider) && !isFinalField(field) && isReceiverDependantAssignable(field, provider)) {
            assert !ElementUtils.isStatic(field);
            valid = true;
        }
        return valid;
    }

    public static boolean isAssigningAssignableField(ExpressionTree node, AnnotationProvider provider) {
        Element fieldElement = TreeUtils.elementFromUse(node);
        if (fieldElement == null) return false;
        return isAssignableField(fieldElement, provider);
    }

    public static boolean isEnclosedByAnonymousClass(Tree tree, AnnotatedTypeFactory atypeFactory) {
        TreePath path = atypeFactory.getPath(tree);
        if (path != null) {
            ClassTree classTree = TreeUtils.enclosingClass(path);
            return classTree != null && InferenceUtil.isAnonymousClass(classTree);
        }
        return false;
    }

    public static AnnotatedDeclaredType getBoundOfEnclosingAnonymousClass(Tree tree, AnnotatedTypeFactory atypeFactory) {
        TreePath path = atypeFactory.getPath(tree);
        if (path == null) {
            return null;
        }
        AnnotatedDeclaredType enclosingType = null;
        Tree newclassTree = TreeUtils.enclosingOfKind(path, Tree.Kind.NEW_CLASS);
        if (newclassTree != null) {
            enclosingType = (AnnotatedDeclaredType) atypeFactory.getAnnotatedType(newclassTree);
        }
        return enclosingType;
    }

    public static AnnotationMirror createEquivalentVarAnnotOfRealQualifier(final AnnotationMirror am) {
        final SlotManager slotManager = InferenceMain.getInstance().getSlotManager();
        ConstantSlot constantSlot = slotManager.createConstantSlot(am);
        return slotManager.getAnnotation(constantSlot);
    }

    public static boolean inStaticScope(TreePath treePath) {
        boolean in = false;
        if (TreeUtils.isTreeInStaticScope(treePath)) {
            in = true;
            // Exclude case in which enclosing class is static
            ClassTree classTree = TreeUtils.enclosingClass(treePath);
            if (classTree != null && classTree.getModifiers().getFlags().contains((Modifier.STATIC))) {
                in = false;
            }
        }
        return in;
    }

    public static boolean isSideEffectingUnaryTree(final UnaryTree tree) {
        return sideEffectingUnaryOperators.contains(tree.getKind());
    }
}
