package fi.evident.apina.java.reader;

import fi.evident.apina.java.model.MethodSignature;
import fi.evident.apina.java.model.type.JavaBasicType;
import fi.evident.apina.java.model.type.JavaType;
import fi.evident.apina.java.model.type.JavaTypeVariable;
import fi.evident.apina.java.model.type.JavaWildcardType;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static fi.evident.apina.java.reader.JavaTypeMatchers.*;
import static fi.evident.apina.java.reader.TypeParser.parseObjectType;
import static fi.evident.apina.java.reader.TypeParser.parseTypeDescriptor;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TypeParserTest {

    private final Map<String,JavaTypeVariable> typeVariableMap = new HashMap<>();

    @Test
    public void parsingJavaTypesWithoutGenericSignatures() {
        assertThat(parseTypeDescriptor("I"), is(basicType(int.class)));
        assertThat(parseTypeDescriptor("V"), is(basicType(void.class)));
        assertThat(parseTypeDescriptor("Z"), is(basicType(boolean.class)));
        assertThat(parseTypeDescriptor("J"), is(basicType(long.class)));

        assertThat(parseTypeDescriptor("Ljava/lang/Integer;"), is(basicType(Integer.class)));
        assertThat(parseTypeDescriptor("Ljava/util/List;"), is(basicType(List.class)));
    }

    @Test
    public void parsingArrayTypesWithoutGenericSignatures() {
        assertThat(parseTypeDescriptor("[I"), is(arrayType(basicType(int.class))));
        assertThat(parseTypeDescriptor("[[[I"), is(arrayType(arrayType(arrayType(basicType(int.class))))));
        assertThat(parseTypeDescriptor("[Ljava/lang/Integer;"), is(arrayType(basicType(Integer.class))));
        assertThat(parseTypeDescriptor("[[Ljava/lang/Integer;"), is(arrayType(arrayType(basicType(Integer.class)))));
    }

    @Test
    public void parsingGenericPrimitiveSignatures() {
        assertThat(parseGenericType("I"), is(basicType(int.class)));
        assertThat(parseGenericType("V"), is(basicType(void.class)));
    }

    @Test
    public void parsingConcreteGenericSignatures() {
        assertThat(parseGenericType("Ljava/util/List<Ljava/lang/Integer;>;"),
                is(genericType(List.class, basicType(Integer.class))));

        assertThat(parseGenericType("Ljava/util/Map<Ljava/lang/Integer;Ljava/lang/String;>;"),
                is(genericType(Map.class, basicType(Integer.class), basicType(String.class))));
    }

    @Test
    public void parsingWildcardTypes() {
        assertThat(parseGenericType("Ljava/util/List<*>;"), is(genericType(List.class, is(JavaWildcardType.unbounded()))));
        assertThat(parseGenericType("Ljava/util/List<+Ljava/lang/String;>;"), is(genericType(List.class, is(JavaWildcardType.extending(new JavaBasicType(String.class))))));
        assertThat(parseGenericType("Ljava/util/List<-Ljava/lang/String;>;"), is(genericType(List.class, is(JavaWildcardType.withSuper(new JavaBasicType(String.class))))));
    }

    @Test
    public void parsingTypeVariables() {
        typeVariableMap.put("T", new JavaTypeVariable("T"));

        assertThat(parseGenericType("TT;"), is(typeVariable("T")));
        assertThat(parseGenericType("Ljava/util/List<TT;>;"), is(genericType(List.class, typeVariable("T"))));
    }

    @Test
    public void parsingGenericArrayTypes() {
        assertThat(parseGenericType("[Ljava/lang/String;"), is(arrayType(basicType(String.class))));
    }

    @Test
    public void parsingGenericArrayTypeForTypeVariable() {
        typeVariableMap.put("A", new JavaTypeVariable("A"));
        assertThat(parseGenericType("[TA;"), is(arrayType(typeVariable("A"))));
    }

    @Test
    public void parsingObjectType() {
        assertThat(parseObjectType("java/lang/Integer"), is(basicType(Integer.class)));
    }

    @Test
    public void parsingNonGenericMethodSignaturesWithSingleParameter() {
        MethodSignature signature = TypeParser.parseMethodDescriptor("(Ljava/lang/Integer;)Ljava/lang/String;");

        assertThat(signature.getReturnType(), is(basicType(String.class)));
        assertThat(signature.getArgumentTypes().size(), is(1));
        assertThat(signature.getArgumentTypes().get(0), is(basicType(Integer.class)));
    }

    @Test
    public void parsingNonGenericMethodSignaturesWithMultipleParameters() {
        MethodSignature signature = TypeParser.parseMethodDescriptor("(Ljava/lang/Class;Ljava/util/function/Function;Ljava/lang/Object;)Ljava/lang/Enum;");

        assertThat(signature.getReturnType(), is(basicType(Enum.class)));
        assertThat(signature.getArgumentTypes().size(), is(3));
        assertThat(signature.getArgumentTypes().get(0), is(basicType(Class.class)));
        assertThat(signature.getArgumentTypes().get(1), is(basicType(Function.class)));
        assertThat(signature.getArgumentTypes().get(2), is(basicType(Object.class)));
    }

    @Test
    public void parsingGenericMethodSignatures_noDeclaredVariables() {
        MethodSignature signature = parseGenericMethodSignature("(Ljava/util/List<Ljava/lang/String;>;)Ljava/util/Set<Ljava/lang/Integer;>;");

        assertThat(signature.getReturnType(), is(genericType(Set.class, basicType(Integer.class))));

        assertThat(signature.getArgumentTypes().size(), is(1));
        assertThat(signature.getArgumentTypes().get(0), is(genericType(List.class, basicType(String.class))));
    }

    @Test
    public void parsingGenericMethodSignatures_declaredVariables() {
        MethodSignature signature = parseGenericMethodSignature("<T:Ljava/io/Serializable;>(TT;)Ljava/util/List<TT;>;");

        assertThat(signature.getArgumentTypes().size(), is(1));
        assertThat(signature.getArgumentTypes().get(0), typeVariable("T", basicType(Serializable.class)));
        assertThat(signature.getReturnType(), is(genericType(List.class, typeVariable("T", basicType(Serializable.class)))));
    }

    @Test
    public void parsingGenericMethodSignatures_variableWithMultipleBounds() {
        MethodSignature signature = parseGenericMethodSignature("<T:Ljava/lang/Cloneable;:Ljava/io/Serializable;>()Ljava/util/List<TT;>;");

        assertThat(signature.getReturnType(), is(genericType(List.class, typeVariable("T", basicType(Cloneable.class), basicType(Serializable.class)))));
    }

    private JavaType parseGenericType(String signature) {
        return TypeParser.parseGenericType(signature, typeVariableMap);
    }

    private MethodSignature parseGenericMethodSignature(String signature) {
        return TypeParser.parseGenericMethodSignature(signature, typeVariableMap);
    }
}