package pico.inference;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import checkers.inference.model.ExistentialVariableSlot;
import checkers.inference.model.Slot;
import checkers.inference.qual.VarAnnot;
import com.sun.tools.javac.code.Symbol;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WildcardTree;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceMain;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.AnnotationLocation;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.VariableSlot;
import checkers.inference.model.tree.ArtificialExtendsBoundTree;
import org.checkerframework.javacutil.TypesUtils;
import pico.common.PICOTypeUtil;

import static pico.typecheck.PICOAnnotationMirrorHolder.*;

public class PICOVariableAnnotator extends VariableAnnotator {

    private boolean generateBottomInequality = true;

    public PICOVariableAnnotator(InferenceAnnotatedTypeFactory typeFactory, AnnotatedTypeFactory realTypeFactory,
                                 InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        super(typeFactory, realTypeFactory, realChecker, slotManager, constraintManager);
    }

    @Override
    protected void handleClassDeclaration(AnnotatedDeclaredType classType, ClassTree classTree) {
        super.handleClassDeclaration(classType, classTree);
        int interfaceIndex = 1;
        for(Tree implementsTree : classTree.getImplementsClause()) {
            final AnnotatedTypeMirror implementsType = inferenceTypeFactory.getAnnotatedTypeFromTypeTree(implementsTree);
            AnnotatedTypeMirror supertype = classType.directSuperTypes().get(interfaceIndex);
            assert supertype.getUnderlyingType() == implementsType.getUnderlyingType();
            visit(supertype, implementsTree);
            interfaceIndex++;
        }
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

        // Insert @Immutable VarAnnot directly to enum bound
//        if (PICOTypeUtil.isEnumOrEnumConstant(bound)) {
//            boundSlot = slotManager.createConstantSlot(IMMUTABLE);
//            classType.addAnnotation(slotManager.getAnnotation(boundSlot));
//            classDeclAnnos.put(classElement, boundSlot);
//            return;
//        }

        Tree classTree = inferenceTypeFactory.declarationFromElement(classElement);
        if (classTree != null) {
            // Have source tree
            if (bound.isAnnotatedInHierarchy(READONLY)) {
                // Have bound annotation -> convert to equivalent ConstantSlot
                boundSlot = slotManager.createConstantSlot(bound.getAnnotationInHierarchy(READONLY));
            } else {
                // No existing annotation -> create new VariableSlot
                boundSlot = createVariable(treeToLocation(classTree));
            }
        } else {
            // No source tree: bytecode classes
            if (bound.isAnnotatedInHierarchy(READONLY)) {
                // Have bound annotation in stub file
                boundSlot = slotManager.createConstantSlot(bound.getAnnotationInHierarchy(READONLY));
            } else {
                // No stub file
                if (PICOTypeUtil.isImplicitlyImmutableType(classType)) {
                    // Implicitly immutable
                    boundSlot = slotManager.createConstantSlot(IMMUTABLE);
                } else {
                    // None of the above applies: use conservative @Mutable
                    boundSlot = slotManager.createConstantSlot(MUTABLE);
                }
            }
        }
        classType.addAnnotation(slotManager.getAnnotation(boundSlot));
        classDeclAnnos.put(classElement, boundSlot);
    }

    @Override
    protected VariableSlot getOrCreateDeclBound(AnnotatedDeclaredType type) {
        TypeElement classDecl = (TypeElement) type.getUnderlyingType().asElement();

        VariableSlot declSlot = classDeclAnnos.get(classDecl);
        if (declSlot == null) {
            // if a explicit annotation presents on the class DECL, use that directly
            if (type.isDeclaration() && type.isAnnotatedInHierarchy(READONLY) && !type.hasAnnotation(READONLY)) {
                VariableSlot constantSlot = (VariableSlot) slotManager.getSlot(type.getAnnotationInHierarchy(READONLY));
//                TypeElement classDecl = (TypeElement) type.getUnderlyingType().asElement();
                classDeclAnnos.put(classDecl, constantSlot);
//                // avoid duplicate annos
//                type.removeAnnotationInHierarchy(READONLY);
                return constantSlot;
            }

            // new class tree of an anonymous class is always visited before (enclosing tree).
            // slot should be generated then.
            // use that slot and avoid generating a new slot.
            // push this change to inference IFF the slot on new class have same requirement with class bound
            // e.g. existence slot on new class tree?
            if (TypesUtils.isAnonymous(type.getUnderlyingType())) {
                assert type.hasAnnotation(VarAnnot.class);
                return (VariableSlot) slotManager.getSlot(type.getAnnotation(VarAnnot.class));
            }
        }
        return super.getOrCreateDeclBound(type);
    }

    @Override
    protected void handleExplicitExtends(Tree extendsTree) {
        // PICO cannot use base extends handling: not simply subtype relationship because of RDM
        // Constraints already generated in processClassTree
    }

    @Override
    public void storeElementType(Element element, AnnotatedTypeMirror atm) {
        // this method is override the behavior of super.handleClassDeclaration before storing
        // find a better way

        Slot slot = slotManager.getVariableSlot(atm);
        // do not use potential slot generated on the class decl annotation
        // PICO always have a annotation on the class bound, so Existential should always exist
        // TODO make VariableAnnotator::getOrCreateDeclBound protected and override that instead of this method
        if (element instanceof Symbol.ClassSymbol && slot instanceof ExistentialVariableSlot) {
            AnnotationMirror potential = slotManager.getAnnotation(((ExistentialVariableSlot) slot).getPotentialSlot());
            atm.replaceAnnotation(potential);
        }

        // If an explicit bound exists, the annotator will still place a constant slot on the bound,
        // which will considered invalid by CF.
        // Maybe not putting an anno at all during bound slot generation would be better?
        if (atm.hasAnnotation(VarAnnot.class) && atm.isAnnotatedInHierarchy(READONLY)) {
            atm.removeAnnotationInHierarchy(READONLY);
        }
        super.storeElementType(element, atm);
    }

    // Don't generate subtype constraint between use type and bound type
    @Override
    protected void handleInstantiationConstraint(AnnotatedTypeMirror.AnnotatedDeclaredType adt, VariableSlot instantiationSlot, Tree tree) {
        return;
    }

    @Override
    protected VariableSlot addPrimaryVariable(AnnotatedTypeMirror atm, Tree tree) {
//        if (PICOTypeUtil.isEnumOrEnumConstant(atm)) {
//            // Don't add new VarAnnot to type use of enum type
//            PICOTypeUtil.applyConstant(atm, IMMUTABLE);
//        }
        if (atm instanceof AnnotatedTypeMirror.AnnotatedNullType) {
            PICOTypeUtil.applyConstant(atm, BOTTOM);
        }
        return super.addPrimaryVariable(atm, tree);
    }

    // Generates inequality constraint between every strict VariableSlot and @Bottom so that @Bottom is not inserted
    // back to source code, but can be within the internal state because of dataflow refinement
    @Override
    protected VariableSlot createVariable(AnnotationLocation location) {
        VariableSlot varSlot = super.createVariable(location);
        // Forbid any explicit use of @Bottom to be inserted back to source code(no VariableSlot instance is inferred
        // @Bottom)
        if (generateBottomInequality) {
            constraintManager.addInequalityConstraint(varSlot, slotManager.createConstantSlot(BOTTOM));
            constraintManager.addInequalityConstraint(varSlot, slotManager.createConstantSlot(POLY_MUTABLE));
        }
        return varSlot;
    }

    // Copied from super implementation
    @Override
    protected boolean handleWasRawDeclaredTypes(AnnotatedDeclaredType adt) {
        if (adt.wasRaw() && adt.getTypeArguments().size() != 0) {
            // the type arguments should be wildcards AND if I get the real type of "tree"
            // it corresponds to the declaration of adt.getUnderlyingType
            Element declarationEle = adt.getUnderlyingType().asElement();
            final AnnotatedDeclaredType declaration =
                    (AnnotatedDeclaredType) inferenceTypeFactory.getAnnotatedType(declarationEle);

            final List<AnnotatedTypeMirror> declarationTypeArgs = declaration.getTypeArguments();
            final List<AnnotatedTypeMirror> rawTypeArgs = adt.getTypeArguments();

            for (int i = 0; i < declarationTypeArgs.size(); i++) {

                if (InferenceMain.isHackMode(rawTypeArgs.get(i).getKind() != TypeKind.WILDCARD)) {
                    return false;
                }

                final AnnotatedTypeMirror.AnnotatedWildcardType rawArg = (AnnotatedTypeMirror.AnnotatedWildcardType) rawTypeArgs.get(i);

                // The only difference starts: instead of copying bounds of declared type variable to
                // type argument wildcard bound, apply default @Mutable(of course equivalent VarAnnot)
                // just like the behaviour in typechecking side.
                // Previsouly, the behaviour is: "E extends @Readonly Object super @Bottom null".
                // Type argument is "? extends Object", so it became "? extends @Readonly Object".
                // This type argument then flows to local variable, and passed as actual method receiver.
                // Since declared receiver is defaulted to @Mutable, it caused inference to give no solution.
                rawArg.getExtendsBound().addMissingAnnotations(
                        Arrays.asList(PICOTypeUtil.createEquivalentVarAnnotOfRealQualifier(MUTABLE)));
                rawArg.getSuperBound().addMissingAnnotations(
                        Arrays.asList(PICOTypeUtil.createEquivalentVarAnnotOfRealQualifier(BOTTOM)));
                // The only different ends
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Void visitWildcard(AnnotatedTypeMirror.AnnotatedWildcardType wildcardType, Tree tree) {
        if (!(tree instanceof WildcardTree)) {
            if (tree instanceof AnnotatedTypeTree) {
                tree = ((AnnotatedTypeTree) tree).getUnderlyingType();
            }
            if (!(tree instanceof WildcardTree)) {
                throw new IllegalArgumentException("Wildcard type ( " + wildcardType + " ) associated " +
                        "with non-WildcardTree ( " + tree + " ) ");
            }
        }

        final WildcardTree wildcardTree = (WildcardTree) tree;
        final Tree.Kind wildcardKind = wildcardTree.getKind();
        if (wildcardKind == Tree.Kind.UNBOUNDED_WILDCARD) {
            boolean prev = generateBottomInequality;
            generateBottomInequality = false;
            // Visit super bound, use the wild card type tree to represents the superbound.
            addPrimaryVariable(wildcardType.getSuperBound(), tree);
            generateBottomInequality = prev;

            // Visit extend bound, construct an artificial extends bound tree to represent the extendbound.
            ArtificialExtendsBoundTree artificialExtendsBoundTree = new ArtificialExtendsBoundTree(wildcardTree);
            addPrimaryVariable(wildcardType.getExtendsBound(), artificialExtendsBoundTree);

        } else if (wildcardKind == Tree.Kind.EXTENDS_WILDCARD) {
            boolean prev = generateBottomInequality;
            generateBottomInequality = false;
            addPrimaryVariable(wildcardType.getSuperBound(), tree);
            generateBottomInequality = prev;

            visit(wildcardType.getExtendsBound(), ((WildcardTree) tree).getBound());

        } else if (wildcardKind == Tree.Kind.SUPER_WILDCARD) {
            addPrimaryVariable(wildcardType.getExtendsBound(), tree);

            boolean prev = generateBottomInequality;
            generateBottomInequality = false;
            visit(wildcardType.getSuperBound(), ((WildcardTree) tree).getBound());
            generateBottomInequality = prev;
        }

        return null;
    }

    @Override
    public void handleBinaryTree(AnnotatedTypeMirror atm, BinaryTree binaryTree) {
        if (atm.isAnnotatedInHierarchy(varAnnot)) {
            // Happens for binary trees whose atm is implicitly immutable and already handled by
            // PICOInferencePropagationTreeAnnotator
            return;
        }
        super.handleBinaryTree(atm, binaryTree);
    }

    public AnnotationMirror getClassDeclAnno(Element ele) {
        if (classDeclAnnos.get(ele) != null) {
            return slotManager.getAnnotation(classDeclAnnos.get(ele));
        }
        return null;
    }


    @Override
    protected void addDeclarationConstraints(VariableSlot declSlot, VariableSlot instanceSlot) {
        // RDM-related constraints cannot use subtype.
        // Necessary constraints added in visitor instead.
    }

}
