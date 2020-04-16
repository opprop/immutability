package pico.inference;

import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreePath;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.*;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.Tree;

import pico.typecheck.PICOAnnotatedTypeFactory;
import pico.typecheck.PICOAnnotatedTypeFactory.PICODefaultForTypeAnnotator;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOTypeAnnotator;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOPropagationTreeAnnotator;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOTreeAnnotator;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOTypeAnnotator;
import pico.typecheck.PICOTypeUtil;
import pico.typecheck.PICOViewpointAdapter;
import qual.Bottom;
import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

/**
 * PICOInferenceRealTypeFactory exists because: 1)PICOAnnotatedTypeFactory is not subtype of
 * BaseAnnotatedTypeFactory. 2) In inference side, PICO only supports reduced version of
 * mutability qualifiers. 3) In inference side, PICO doesn't need to care initialization hierarchy.
 * We have all the logic that are in PICOAnnotatedTypeFactory except those that belong
 * to InitializationAnnotatedTypeFactory as if there is only one mutability qualifier hierarchy.
 * This class has lots of copied code from PICOAnnotatedTypeFactory. The two should be in sync.
 */
public class PICOInferenceRealTypeFactory extends BaseAnnotatedTypeFactory implements PublicViewpointAdapter {

    public PICOInferenceRealTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker, useFlow);
        if (READONLY != null) {
            addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
        }
        postInit();
    }

    /**Only support mutability qualifier hierarchy*/
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        PolyMutable.class,
                        Readonly.class,
                        Mutable.class,
                        ReceiverDependantMutable.class,
                        Immutable.class,
                        Bottom.class));
    }

    // TODO Remove this temporary viewpoint adaptor
    @Override
    protected AbstractViewpointAdapter createViewpointAdapter() {
        return new PICOViewpointAdapter(this);
    }

    /**Annotators are executed by the added order. Same for Type Annotator*/
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new PICOPropagationTreeAnnotator(this),
                new LiteralTreeAnnotator(this),
                new PICOSuperClauseAnnotator(this),
                new PICOTreeAnnotator(this));
    }

    // TODO Refactor super class to remove this duplicate code
    @Override
    protected TypeAnnotator createTypeAnnotator() {
        /*Copied code start*/
        List<TypeAnnotator> typeAnnotators = new ArrayList<>();
        RelevantJavaTypes relevantJavaTypes =
                checker.getClass().getAnnotation(RelevantJavaTypes.class);
        if (relevantJavaTypes != null) {
            Class<?>[] classes = relevantJavaTypes.value();
            // Must be first in order to annotated all irrelevant types that are not explicilty
            // annotated.
            typeAnnotators.add(
                    new IrrelevantTypeAnnotator(
                            this, getQualifierHierarchy().getTopAnnotations(), classes));
        }
        typeAnnotators.add(new PropagationTypeAnnotator(this));
        /*Copied code ends*/
        // Adding order is important here. Because internally type annotators are using addMissingAnnotations()
        // method, so if one annotator already applied the annotations, the others won't apply twice at the
        // same location
        typeAnnotators.add(new PICOTypeAnnotator(this));
        typeAnnotators.add(new PICODefaultForTypeAnnotator(this));
        return new ListTypeAnnotator(typeAnnotators);
    }

    /** TODO If the dataflow refines the type as bottom, should we allow such a refinement? If we allow it,
     PICOValidator will give an error if it begins to enforce @Bottom is not used*/
/*    @Override
    protected void applyInferredAnnotations(AnnotatedTypeMirror type, PICOValue as) {
        super.applyInferredAnnotations(type, as);
        // What to do if refined type is bottom?
    }*/

    /**Forbid applying top annotations to type variables if they are used on local variables*/
    @Override
    public boolean getShouldDefaultTypeVarLocals() {
        return false;
    }

    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        PICOTypeUtil.addDefaultForField(this, type, elt);
        PICOTypeUtil.defaultConstructorReturnToClassBound(this, elt, type);
        PICOTypeUtil.applyImmutableToEnumAndEnumConstant(type);
        super.addComputedTypeAnnotations(elt, type);
    }

    /**This method gets lhs WITH flow sensitive refinement*/
    // TODO This method is completely copied from PICOAnnotatedTypeFactory
    @Override
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
        AnnotatedTypeMirror result = null;
        boolean oldShouldCache = shouldCache;
        // Don't cache the result because getAnnotatedType(lhsTree) could
        // be called from elsewhere and would expect flow-sensitive type refinements.
        shouldCache = false;
        switch (lhsTree.getKind()) {
            case VARIABLE:
            case IDENTIFIER:
            case MEMBER_SELECT:
            case ARRAY_ACCESS:
                result = getAnnotatedType(lhsTree);
                break;
            default:
                if (TreeUtils.isTypeTree(lhsTree)) {
                    // lhsTree is a type tree at the pseudo assignment of a returned expression to declared return type.
                    result = getAnnotatedType(lhsTree);
                } else {
                    throw new BugInCF(
                            "GenericAnnotatedTypeFactory: Unexpected tree passed to getAnnotatedTypeLhs. "
                                    + "lhsTree: "
                                    + lhsTree
                                    + " Tree.Kind: "
                                    + lhsTree.getKind());
                }
        }
        shouldCache = oldShouldCache;

        return result;
    }

    @Override
    protected DefaultQualifierForUseTypeAnnotator createDefaultForUseTypeAnnotator() {
        return new PICOAnnotatedTypeFactory.PICOQualifierForUseTypeAnnotator(this);
    }

    @Override
    public AnnotatedTypeMirror getTypeOfExtendsImplements(Tree clause) {
        // add default anno from class main qual, if no qual present
        AnnotatedTypeMirror enclosing = getAnnotatedType(TreeUtils.enclosingClass(getPath(clause)));
        AnnotationMirror mainBound = enclosing.getAnnotationInHierarchy(READONLY);
        AnnotatedTypeMirror fromTypeTree = this.fromTypeTree(clause);
        if (!fromTypeTree.isAnnotatedInHierarchy(READONLY)) {
            fromTypeTree.addAnnotation(mainBound);
        }

        // for FBC quals
//        Set<AnnotationMirror> bound = this.getTypeDeclarationBounds(fromTypeTree.getUnderlyingType());
//        fromTypeTree.addMissingAnnotations(bound);
        return fromTypeTree;
    }

    public ExtendedViewpointAdapter getViewpointAdapter() {
        return (ExtendedViewpointAdapter) viewpointAdapter;
    }

    @Override
    protected Set<? extends AnnotationMirror> getDefaultTypeDeclarationBounds() {
        return Collections.singleton(MUTABLE);
    }

    @Override
    public Set<AnnotationMirror> getTypeDeclarationBounds(TypeMirror type) {
        // TODO too awkward. maybe overload isImplicitlyImmutableType
        if (PICOTypeUtil.isImplicitlyImmutableType(toAnnotatedType(type, false))) {
            return Collections.singleton(IMMUTABLE);
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return Collections.singleton(READONLY); // if decided to use vpa for array, return RDM.
        }
        return super.getTypeDeclarationBounds(type);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        return super.getAnnotatedType(tree);
    }

    public static class PICOSuperClauseAnnotator extends TreeAnnotator {

        public PICOSuperClauseAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitIdentifier(IdentifierTree identifierTree, AnnotatedTypeMirror annotatedTypeMirror) {
            TreePath path = atypeFactory.getPath(identifierTree);

            if (path != null && (!annotatedTypeMirror.isAnnotatedInHierarchy(READONLY) | atypeFactory.getQualifierHierarchy().findAnnotationInHierarchy(TreeUtils.typeOf(identifierTree).getAnnotationMirrors(), READONLY) == null) &&
                    TreeUtils.isClassTree(path.getParentPath().getLeaf())) {
                AnnotatedTypeMirror enclosing = atypeFactory.getAnnotatedType(TreeUtils.enclosingClass(path));
                AnnotationMirror mainBound = enclosing.getAnnotationInHierarchy(READONLY);
                annotatedTypeMirror.replaceAnnotation(mainBound);
                System.err.println("ANNOT: ADDED DEFAULT FOR: " + annotatedTypeMirror);
            }
            return super.visitIdentifier(identifierTree, annotatedTypeMirror);
        }
    }
}
