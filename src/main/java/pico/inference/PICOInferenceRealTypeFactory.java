package pico.inference;

import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.IrrelevantTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.Tree;

import pico.typecheck.PICOAnnotatedTypeFactory.PICOImplicitsTypeAnnotator;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOPropagationTreeAnnotator;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOTreeAnnotator;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOTypeAnnotator;
import pico.typecheck.PICOTypeUtil;
import pico.typecheck.PICOViewpointAdapter;
import qual.Bottom;
import qual.Immutable;
import qual.Mutable;
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
public class PICOInferenceRealTypeFactory extends BaseAnnotatedTypeFactory {

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
                new ImplicitsTreeAnnotator(this),
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
        typeAnnotators.add(new PICOImplicitsTypeAnnotator(this));
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

    // Copied from PICOAnnotatedTypeFactory
    @Override
    protected void annotateInheritedFromClass(AnnotatedTypeMirror type, Set<AnnotationMirror> fromClass) {
        // If interitted from class element is @Mutable or @Immutable, then apply this annotation to the usage type
        if (fromClass.contains(MUTABLE) || fromClass.contains(IMMUTABLE)) {
            super.annotateInheritedFromClass(type, fromClass);
            return;
        }
        // If interitted from class element is @ReceiverDependantMutable, then don't apply and wait for @Mutable
        // (default qualifier in hierarchy to be applied to the usage type). This is to avoid having @ReceiverDependantMutable
        // on type usages as a default behaviour. By default, @Mutable is better used as the type for usages that
        // don't have explicit annotation.
        return;// Don't add annotations from class element
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
}
