package pico.typecheck;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import qual.ReceiverDependantMutable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 * Created by mier on 29/09/17.
 */
public class PICOTypeValidator extends BaseTypeValidator {
    public PICOTypeValidator(BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    private boolean isInStaticContext(){
        boolean isstatic = false;
        MethodTree meth = TreeUtils.enclosingMethod(visitor.getCurrentPath());
        if(meth != null){
            ExecutableElement methel = TreeUtils.elementFromDeclaration(meth);
            isstatic = ElementUtils.isStatic(methel);
        } else {
            BlockTree blcktree = TreeUtils.enclosingTopLevelBlock(visitor.getCurrentPath());
            if (blcktree != null) {
                isstatic = blcktree.isStatic();
            } else {
                VariableTree vartree = TreeUtils.enclosingVariable(visitor.getCurrentPath());
                if (vartree != null) {
                    ModifiersTree mt = vartree.getModifiers();
                    isstatic = mt.getFlags().contains(Modifier.STATIC);
                }
            }
        }
        return isstatic;
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        if (isInStaticContext() && type.hasAnnotation(ReceiverDependantMutable.class)) {
            // TODO Remove duplicate warnings
            checker.report(
                    Result.failure(
                            "static.receiverdependantmutable.forbidden", type), tree);
        }
        return super.visitDeclared(type, tree);
    }
}
