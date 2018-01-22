package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceMain;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.AnnotationLocation;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.VariableSlot;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import pico.typecheck.PICOTypeUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;

public class PICOVariableAnnotator extends VariableAnnotator {

    public PICOVariableAnnotator(InferenceAnnotatedTypeFactory typeFactory, AnnotatedTypeFactory realTypeFactory,
                                 InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        super(typeFactory, realTypeFactory, realChecker, slotManager, constraintManager);
    }

    @Override
    protected void handleClassDeclarationBound(AnnotatedDeclaredType classType) {
        TypeElement classElement = (TypeElement) classType.getUnderlyingType().asElement();
        if (classDeclAnnos.containsKey(classElement)) {
            classType.addAnnotation(slotManager.getAnnotation(classDeclAnnos.get(classElement)));
            classType.addAnnotation(READONLY);
            return;
        }
        AnnotatedDeclaredType bound = inferenceTypeFactory.fromElement(classElement);

        VariableSlot boundSlot;

        Tree classTree = inferenceTypeFactory.declarationFromElement(classElement);
        if (classTree != null) {
            // Have source tree
            if (bound.isAnnotatedInHierarchy(READONLY)) {
                // Have bound annotation -> convert to equivalent ConstantSlot
                boundSlot = createConstant(bound.getAnnotationInHierarchy(READONLY));
            } else {
                // No existing annotation -> create new VariableSlot
                boundSlot = createVariable(treeToLocation(classTree));
            }
        } else {
            // No source tree: bytecode classes
            if (bound.isAnnotatedInHierarchy(READONLY)) {
                // Have bound annotation in stub file
                boundSlot = createConstant(bound.getAnnotationInHierarchy(READONLY));
            } else {
                // No stub file
                if (PICOTypeUtil.isImplicitlyImmutableType(classType)) {
                    // Implicitly immutable
                    boundSlot = createConstant(IMMUTABLE);
                } else {
                    // None of the above applies: use conservative @Mutable
                    boundSlot = createConstant(MUTABLE);
                }
            }
        }
        classType.addAnnotation(slotManager.getAnnotation(boundSlot));
        classDeclAnnos.put(classElement, boundSlot);
    }

    // Don't generate subtype constraint between use type and bound type
    @Override
    protected void handleInstantiationConstraint(AnnotatedTypeMirror.AnnotatedDeclaredType adt, VariableSlot instantiationSlot, Tree tree) {
        return;
    }

    // Generates inequality constraint between every strict VariableSlot and @Bottom so that @Bottom is not inserted
    // back to source code, but can be within the internal state because of dataflow refinement
    @Override
    protected VariableSlot createVariable(AnnotationLocation location) {
        VariableSlot varSlot = super.createVariable(location);
        // Forbid any explicit use of @Bottom to be inserted back to source code(no VariableSlot instance is inferred
        // @Bottom)
        constraintManager.addInequalityConstraint(varSlot, slotManager.createConstantSlot(BOTTOM));
        return varSlot;
    }

    // Not annotate extends bound of class declaration
    // TODO Infer.java still gets inserted VarAnnot on extends clause. Need to furthur investigate extend problem
    @Override
    protected void handleClassDeclaration(AnnotatedTypeMirror.AnnotatedDeclaredType classType, ClassTree classTree) {
        // Below is copied
        // TODO: NOT SURE THIS HANDLES MEMBER SELECT CORRECTLY
        int interfaceIndex = 1;
        for(Tree implementsTree : classTree.getImplementsClause()) {
            final AnnotatedTypeMirror implementsType = inferenceTypeFactory.getAnnotatedTypeFromTypeTree(implementsTree);
            AnnotatedTypeMirror supertype = classType.directSuperTypes().get(interfaceIndex);
            assert supertype.getUnderlyingType() == implementsType.getUnderlyingType();
            visit(supertype, implementsTree);
            interfaceIndex++;
        }

        if (InferenceMain.isHackMode(
                (classType.getTypeArguments().size() != classTree.getTypeParameters().size()))) {
            return;
        }

        visitTogether(classType.getTypeArguments(), classTree.getTypeParameters());

        handleClassDeclarationBound(classType);

        // before we were relying on trees but the ClassTree has it's type args erased
        // when the compiler moves on to the next class
        Element classElement = classType.getUnderlyingType().asElement();
        storeElementType(classElement, classType);
        // Above is copied
    }
}
