package qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

import org.checkerframework.framework.qual.TypeKind;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;

@SubtypeOf({Readonly.class})
@QualifierForLiterals({LiteralKind.PRIMITIVE, LiteralKind.STRING})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(types={Enum.class, String.class, Double.class, Boolean.class, Byte.class,
        Character.class, Float.class, Integer.class, Long.class, Short.class, Number.class,
        BigDecimal.class, BigInteger.class},
        typeKinds = { TypeKind.INT, TypeKind.BYTE, TypeKind.SHORT, TypeKind.BOOLEAN,
                TypeKind.LONG, TypeKind.CHAR, TypeKind.FLOAT, TypeKind.DOUBLE })
public @interface Immutable {}
