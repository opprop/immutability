package qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SubtypeOf({Readonly.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(typeNames={String.class}, literals = { LiteralKind.ALL},
        types = { TypeKind.INT, TypeKind.BYTE, TypeKind.SHORT, TypeKind.BOOLEAN,
                TypeKind.LONG, TypeKind.CHAR, TypeKind.FLOAT, TypeKind.DOUBLE })
public @interface Immutable {}
