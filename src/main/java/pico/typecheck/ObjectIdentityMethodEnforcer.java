package pico.typecheck;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import qual.Assignable;
import qual.Mutable;
import qual.Readonly;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

public class ObjectIdentityMethodEnforcer extends TreePathScanner<Void, Void> {

    private PICOAnnotatedTypeFactory typeFactory;
    private BaseTypeChecker checker;

    private ObjectIdentityMethodEnforcer(PICOAnnotatedTypeFactory typeFactory, BaseTypeChecker checker) {
        this.typeFactory = typeFactory;
        this.checker = checker;
    }

    // Main entry
    public static void check(TreePath statement, PICOAnnotatedTypeFactory typeFactory, BaseTypeChecker checker) {
        if (statement == null) return;
        ObjectIdentityMethodEnforcer asfchecker = new
                ObjectIdentityMethodEnforcer(typeFactory, checker);
        asfchecker.scan(statement, null);
    }

    // checks method invocation with forms: foo(), this.foo(), super.foo(). All the other forms
    // are skipped： f.foo(), this.f.foo(), a.b.foo()
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
        Element elt = TreeUtils.elementFromUse(node);
        checkMethod(node, elt);
        return super.visitMethodInvocation(node, aVoid);
    }

    private void checkMethod(MethodInvocationTree node, Element elt) {
        assert elt instanceof ExecutableElement;
        if (ElementUtils.isStatic(elt)) {
            return;// Doesn't check static method invocation because it doesn't access instance field
        }
        if (node.getMethodSelect() instanceof MemberSelectTree
                && !"this".contentEquals(((MemberSelectTree)node.getMethodSelect()).getExpression().toString())
                && !"super".contentEquals(((MemberSelectTree)node.getMethodSelect()).getExpression().toString())) {
            return;// Only check method invocation when the invoked method is on the receiver itself(super is also on itself)
        }
        // Checks two types of method invocation: e.g. foo() or this.foo()/super.foo().
        if (!PICOTypeUtil.isObjectIdentityMethod((ExecutableElement) elt, typeFactory)) {
            // Report error since invoked method is not only dependant on abstract state fields
            checker.report(Result.failure("object.identity.method.invocation.invalid", elt), node);
        }
    }

    // For field accesses with no explicit "this": e.g. f=2;l=f; f.hashCode() etc.
    @Override
    public Void visitIdentifier(IdentifierTree node, Void aVoid) {
        Element elt = TreeUtils.elementFromUse(node);
        checkField(node, elt);
        return super.visitIdentifier(node, aVoid);
    }

    // For field access with explicit "this" keyword: e.g. this.f=2;l=this.f; this.f.hashCode()
    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void aVoid) {
        if (node.getExpression() != null && node.getExpression().toString().contentEquals("this")) {
            Element elt = TreeUtils.elementFromUse(node);
            checkField(node, elt);
        }
        return super.visitMemberSelect(node, aVoid);
    }

    private void checkField(Tree node, Element elt) {
        if (elt != null && elt.getKind() == ElementKind.FIELD) {
            if (ElementUtils.isStatic(elt)) {
                checker.report(Result.failure("object.identity.static.field.access.forbidden", elt), node);
            } else {
                if (!isInAbstractState(elt, typeFactory)) {
                    // Report error since accessed field is not within abstract state
                    checker.report(Result.failure("object.identity.field.access.invalid", elt), node);
                }
            }
        }
    }

    // Test if a field is in abstract state or not
    private boolean isInAbstractState(Element elt, PICOAnnotatedTypeFactory typeFactory) {
        boolean in = true;
        if (typeFactory.getDeclAnnotation(elt, Assignable.class) != null) {
            in = false;
        } else if (typeFactory.getAnnotatedType(elt).hasAnnotation(Mutable.class)) {
            in = false;
        } else if (typeFactory.getAnnotatedType(elt).hasAnnotation(Readonly.class)) {
            in = false;
        }

        return in;
    }
}
