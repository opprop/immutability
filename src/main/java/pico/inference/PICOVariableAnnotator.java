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

import javax.lang.model.element.Element;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;

public class PICOVariableAnnotator extends VariableAnnotator {

    public PICOVariableAnnotator(InferenceAnnotatedTypeFactory typeFactory, AnnotatedTypeFactory realTypeFactory,
                                 InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        super(typeFactory, realTypeFactory, realChecker, slotManager, constraintManager);
    }

//    // Don't generate class declaration bound
//    @Override
//    protected void handleClassDeclarationBound(AnnotatedTypeMirror.AnnotatedDeclaredType classType) {
//        return;
//    }
//
//    // Don't generate subtype constraint between use type and bound type
//    @Override
//    protected void handleInstantiationConstraint(AnnotatedTypeMirror.AnnotatedDeclaredType adt, VariableSlot instantiationSlot, Tree tree) {
//        return;
//    }

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
