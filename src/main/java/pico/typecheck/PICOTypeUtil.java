package pico.typecheck;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import qual.Immutable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PICOTypeUtil {

    private static boolean isInTypesOfImplicitForOfImmutable(AnnotatedTypeMirror atm) {
        ImplicitFor implicitFor = Immutable.class.getAnnotation(ImplicitFor.class);
        assert implicitFor != null;
        assert implicitFor.types() != null;
        for (TypeKind typeKind : implicitFor.types()) {
            if (typeKind == atm.getKind()) return true;
        }
        return false;
    }

    private static boolean isInTypeNamesOfImplicitForOfImmutable(AnnotatedTypeMirror atm) {
        if (atm.getKind() != TypeKind.DECLARED) {
            return false;
        }
        ImplicitFor implicitFor = Immutable.class.getAnnotation(ImplicitFor.class);
        assert implicitFor != null;
        assert implicitFor.typeNames() != null;
        Class<?>[] typeNames = implicitFor.typeNames();
        String fqn = TypesUtils.getQualifiedName((DeclaredType) atm.getUnderlyingType()).toString();
        for (int i = 0; i < typeNames.length; i++) {
            if (typeNames[i].getCanonicalName().toString().contentEquals(fqn)) return true;
        }
        return false;
    }

    /**Method to determine if the underlying type is implicitly immutable. This method is consistent
     * with the types and typeNames that are in @ImplicitFor in the definition of @Immutable qualifier*/
    public static boolean isImplicitlyImmutableType(AnnotatedTypeMirror atm) {
        return isInTypesOfImplicitForOfImmutable(atm) || isInTypeNamesOfImplicitForOfImmutable(atm);
    }

    /**
     * Returns the bound of type declaration enclosing the node.
     *
     * If no annotation exists on type declaration, defaults to @Mutable instead of returning null.
     * Returning null represents intended cases that bound annotation should not be considered.
     * For example, anonymous classes has null bound annotation(TODO Is this really true??);
     *
     * This method simply gets/defaults annotation on bounds of classes, but
     * doesn't validate the correctness of the annotation. They are validated in PICOVisitor#processClassTree()
     * method.
     *
     * @param node tree whose enclosing type declaration's bound annotation is to be extracted
     * @param atypeFactory pico type factory
     * @return annotation on the bound of enclosing type declaration
     */
    public static AnnotationMirror getBoundAnnotationOnEnclosingTypeDeclaration(Tree node, PICOAnnotatedTypeFactory atypeFactory) {
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
            return getBoundAnnotationOnTypeDeclaration(typeElement, atypeFactory);
        }

        return null;
    }

    // TODO This method does very similar job with AnnotatedTypeFactory#getAnnotatedType(Element). Maybe should call
    // that method inside this method and add additional logic here
    public static AnnotationMirror getBoundAnnotationOnTypeDeclaration(TypeElement typeElement, PICOAnnotatedTypeFactory atypeFactory) {
        // Ignore anonymous classes. It doesn't have bound annotation. The annotation on new instance
        // creation is wrongly passed here as bound annotation. As a result, if anonymous class is instantiated
        // with @Immutable instance, it gets warned "constructor.return.incompatible" because anonymous
        // class only has default @Mutable constructor
        if (typeElement.toString().contains("anonymous")) return null;// TODO Return bound annotation on super class maybe

        // If there is bound annotation on type element, then addMissingAnnotations() won't affect it and it falls through and is returned
        AnnotatedDeclaredType bound = atypeFactory.fromElement(typeElement); // Reads bound annotation from source code or stub files

        // TODO It's a bit strange that bound annotations on java.lang.Object and implicilty immutable types
        // are not specified in the stub file. Because bound annotation on stub file causes use type to be
        // also the same bound annotation. But for java.lang.Object, we want it to be defaulted to @Readonly
        // everywhere(@ImplicitFor trick to do this). But having it in stub file causes java.lang.Object is
        // defaulted to @rdm, which is not what we want; For implicitly immutable types, having bounds in stub
        // file suppresses type cast warnings, because in base implementation, it checks cast type is whether
        // from element itself. If yes, no matter what the casted type is, the warning is suppressed, which is
        // also not wanted. So we have the logic of determining bounds for different type elements here.
        if (isImplicitlyImmutableType(bound)) {
            bound.addMissingAnnotations(Arrays.asList(atypeFactory.IMMUTABLE));
        } else if (typeElement.toString().equals("java.lang.Object")) {
            // defaults to rdm for java.lang.Object
            bound.addMissingAnnotations(Arrays.asList(atypeFactory.RECEIVERDEPENDANTMUTABLE));
        } else {
            // defaults to all other elements that are: not java.lang.Object; not implicitly immutable types
            // specified in definition of @Immutable qualifier; has no bound annotation on its type element
            // declaration either in source tree or stub file(jdk.astub)
            bound.addMissingAnnotations(Arrays.asList(atypeFactory.MUTABLE));
        }
        return bound.getAnnotationInHierarchy(atypeFactory.READONLY);
    }

    public static List<AnnotationMirror> getBoundAnnotationOnDirectSuperTypeDeclarations(TypeElement current, PICOAnnotatedTypeFactory atypeFactory) {
        List<AnnotationMirror> boundAnnotsOnSupers = new ArrayList<>();
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

        if (supertypecls != null && supertypecls.getKind() != TypeKind.NONE) {
            TypeElement supercls = (TypeElement) ((DeclaredType) supertypecls).asElement();
            boundAnnotsOnSupers.add(getBoundAnnotationOnTypeDeclaration(supercls, atypeFactory));
        }

        for (TypeMirror supertypeitf : current.getInterfaces()) {
            TypeElement superitf = (TypeElement) ((DeclaredType) supertypeitf).asElement();
            boundAnnotsOnSupers.add(getBoundAnnotationOnTypeDeclaration(superitf, atypeFactory));
        }
        return boundAnnotsOnSupers;
    }
}
