/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.github.emmmm9o.javatypes.JavaTypes.*;
import com.github.emmmm9o.javatypes.exceptions.TypeParsingException;

/**
 * Parser for converting Java reflection types into the internal type system.
 * This class handles the conversion of Java classes, methods, fields, and their
 * type information into a format that can be used for generating type definitions
 * in other languages.
 */
public class Parser {
    private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());

    /**
     * Filter interface for determining which classes should be processed.
     */
    @FunctionalInterface
    public interface Filter {
        boolean filter(Class<?> clazz);
    }

    /**
     * Interface for determining if a field can be null.
     */
    @FunctionalInterface
    public interface FieldNullable {
        boolean check(Field field);
    }

    /**
     * Interface for determining if a parameter can be null.
     */
    @FunctionalInterface
    public interface ParameterNullable {
        boolean check(Parameter parameter);
    }

    /**
     * Interface for determining if a method's return value can be null.
     */
    @FunctionalInterface
    public interface MethodNullable {
        boolean check(Method method);
    }

    private final FieldNullable fieldNullableChecker;
    private final ParameterNullable parameterNullableChecker;
    private final MethodNullable methodNullableChecker;
    private final Filter classFilter;
    private final Map<String, JavaType> classMap;
    private final Map<String, Boolean> processedMap;
    private final List<JavaType> processedTypes;
    private final Map<Type, JavaTypeUse> typeCache;

    /**
     * Creates a new Parser with default settings.
     */
    public Parser() {
        this(f -> true, p -> true, m -> true, clazz -> false);
    }

    /**
     * Creates a new Parser with custom settings.
     */
    public Parser(FieldNullable fieldNullable, 
                 ParameterNullable parameterNullable,
                 MethodNullable methodNullable,
                 Filter filter) {
        this.fieldNullableChecker = fieldNullable;
        this.parameterNullableChecker = parameterNullable;
        this.methodNullableChecker = methodNullable;
        this.classFilter = filter;
        this.classMap = new ConcurrentHashMap<>();
        this.processedMap = new ConcurrentHashMap<>();
        this.processedTypes = new ArrayList<>();
        this.typeCache = new ConcurrentHashMap<>();
        initializeEnvironment();
    }

    /**
     * Initializes the basic Java types in the parser's environment.
     */
    private void initializeEnvironment() {
        List<Class<?>> basicTypes = Arrays.asList(
            Object.class, Integer.class, String.class, Class.class,
            Double.class, Float.class, Character.class, Void.class,
            Boolean.class, int.class, boolean.class, float.class,
            double.class, void.class, byte.class, char.class
        );

        for (Class<?> type : basicTypes) {
            registerClass(type);
        }
    }

    /**
     * Registers a class in the parser's type system.
     */
    private void registerClass(Class<?> clazz) {
        JavaType type = JavaType.fromClass(clazz);
        classMap.put(clazz.getName(), type);
        processedMap.put(clazz.getName(), true);
    }

    /**
     * Gets the modifiers for a given modifier int value.
     */
    public static Set<JavaModifier> getModifiers(int value) {
        Set<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        if (Modifier.isPublic(value)) modifiers.add(JavaModifier.Public);
        if (Modifier.isProtected(value)) modifiers.add(JavaModifier.Protected);
        if (Modifier.isPrivate(value)) modifiers.add(JavaModifier.Private);
        if (Modifier.isStatic(value)) modifiers.add(JavaModifier.Static);
        if (Modifier.isFinal(value)) modifiers.add(JavaModifier.Final);
        if (Modifier.isAbstract(value)) modifiers.add(JavaModifier.Abstract);
        return modifiers;
    }

    /**
     * Parses type parameters from a parameterized type.
     */
    public List<JavaTypeUse> parseTypeParameters(Type type) {
        if (type instanceof ParameterizedType paramType) {
            return parseGenerics(paramType);
        }
        return new ArrayList<>();
    }

    /**
     * Gets or creates a JavaTypeUse for a given Type.
     */
    public JavaTypeUse getTypeUse(Type type) {
        return getTypeUse(type, false);
    }

    /**
     * Gets or creates a JavaTypeUse for a given Type, with control over supertype processing.
     */
    public JavaTypeUse getTypeUse(Type type, boolean processSuperTypes) {
        try {
            return typeCache.computeIfAbsent(type, t -> createTypeUse(t, processSuperTypes));
        } catch (Exception e) {
            throw new TypeParsingException("Failed to parse type: " + type, e);
        }
    }

    private JavaTypeUse createTypeUse(Type type, boolean processSuperTypes) {
        if (type instanceof Class<?> clazz) {
            return handleClassType(clazz, processSuperTypes);
        } else if (type instanceof ParameterizedType paramType) {
            return handleParameterizedType(paramType, processSuperTypes);
        } else if (type instanceof TypeVariable<?> typeVar) {
            return handleTypeVariable(typeVar);
        } else if (type instanceof GenericArrayType arrayType) {
            return handleGenericArrayType(arrayType, processSuperTypes);
        } else if (type instanceof WildcardType wildcardType) {
            return handleWildcardType(wildcardType);
        }
        
        throw new TypeParsingException("Unsupported type: " + type.getClass());
    }

    private JavaTypeUse handleClassType(Class<?> clazz, boolean processSuperTypes) {
        return JavaTypeUse.builder()
            .withType(clazz.isArray() ? null : parse(clazz, processSuperTypes))
            .withComponentType(clazz.isArray() ? getTypeUse(clazz.getComponentType(), processSuperTypes) : null)
            .withTypeRef(clazz)
            .build();
    }

    private JavaTypeUse handleParameterizedType(ParameterizedType paramType, boolean processSuperTypes) {
        return JavaTypeUse.builder()
            .withType(parse((Class<?>) paramType.getRawType(), processSuperTypes))
            .withGenerics(parseGenerics(paramType))
            .withTypeRef(paramType)
            .build();
    }

    private JavaTypeUse handleTypeVariable(TypeVariable<?> typeVar) {
        JavaTypeUse typeUse = JavaTypeUse.builder()
            .withTypeGeneric(typeVar.getName())
            .withTypeRef(typeVar)
            .build();
        typeUse.setUpperBounds(getTypeUses(typeVar.getBounds()));
        return typeUse;
    }

    private JavaTypeUse handleGenericArrayType(GenericArrayType arrayType, boolean processSuperTypes) {
        return JavaTypeUse.builder()
            .withComponentType(getTypeUse(arrayType.getGenericComponentType(), processSuperTypes))
            .withTypeRef(arrayType)
            .build();
    }

    private JavaTypeUse handleWildcardType(WildcardType wildcardType) {
        JavaTypeUse typeUse = JavaTypeUse.builder()
            .withTypeGeneric("?")
            .withTypeRef(wildcardType)
            .build();
        typeUse.setUpperBounds(getTypeUses(wildcardType.getUpperBounds()));
        typeUse.setLowerBounds(getTypeUses(wildcardType.getLowerBounds()));
        return typeUse;
    }

    private List<JavaTypeUse> getTypeUses(Type[] types) {
        List<JavaTypeUse> typeUses = new ArrayList<>();
        for (Type type : types) {
            typeUses.add(getTypeUse(type));
        }
        return typeUses;
    }

    /**
     * Parses generic type arguments from a parameterized type.
     */
    public List<JavaTypeUse> parseGenerics(ParameterizedType paramType) {
        return getTypeUses(paramType.getActualTypeArguments());
    }

    /**
     * Parses a class into the internal type system.
     */
    public JavaType parse(Class<?> clazz) {
        return parse(clazz, false);
    }

    /**
     * Parses a class into the internal type system, with control over supertype processing.
     */
    public JavaType parse(Class<?> clazz, boolean processSuperTypes) {
        try {
            JavaType existingType = classMap.get(clazz.getName());
            Boolean isProcessed = processedMap.getOrDefault(clazz.getName(), false);

            if (isProcessed && processSuperTypes) {
                processedMap.put(clazz.getName(), false);
            } else if (existingType != null) {
                return existingType;
            }

            JavaType.Builder builder = JavaType.builder()
                .withName(clazz.getSimpleName())
                .withClasspath(clazz.getName())
                .withClassRef(clazz)
                .withGenerics(getTypeUses(clazz.getTypeParameters()))
                .isInner(clazz.getDeclaringClass() != null);

            JavaType type = builder.build();
            classMap.put(clazz.getName(), type);

            if (classFilter.filter(clazz) && !processSuperTypes) {
                if (!type.isInner()) {
                    processedTypes.add(type);
                }
                processedMap.put(clazz.getName(), true);
                return type;
            }

            parseTypeHierarchy(type, clazz, processSuperTypes);
            parseMembers(type, clazz);

            return type;
        } catch (Exception e) {
            throw new TypeParsingException("Failed to parse class: " + clazz.getName(), e);
        }
    }

    private void parseTypeHierarchy(JavaType type, Class<?> clazz, boolean processSuperTypes) {
        Type superclass = clazz.getGenericSuperclass();
        if (superclass != null) {
            type.setSuperType(getTypeUse(superclass, true));
        }

        for (Type iface : clazz.getGenericInterfaces()) {
            type.addInterface(getTypeUse(iface, true));
        }

        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            type.addInnerClass(parse(innerClass));
        }
    }

    private void parseMembers(JavaType type, Class<?> clazz) {
        parseFields(type, clazz);
        parseConstructors(type, clazz);
        parseMethods(type, clazz);
    }

    private void parseFields(JavaType type, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            type.addField(JavaField.builder()
                .withType(getTypeUse(field.getGenericType()))
                .withName(field.getName())
                .withModifiers(getModifiers(field.getModifiers()))
                .isNullable(fieldNullableChecker.check(field))
                .build());
        }
    }

    private void parseConstructors(JavaType type, Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            type.addConstructor(createMethodFromConstructor(constructor));
        }
    }

    private void parseMethods(JavaType type, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            type.addMethod(createMethodFromMethod(method));
        }
    }

    private JavaMethod createMethodFromConstructor(Constructor<?> constructor) {
        return JavaMethod.builder()
            .withModifiers(getModifiers(constructor.getModifiers()))
            .withGenerics(getTypeUses(constructor.getTypeParameters()))
            .isVarArgs(constructor.isVarArgs())
            .withParameters(createParameters(constructor.getParameters()))
            .withConstructorRef(constructor)
            .build();
    }

    private JavaMethod createMethodFromMethod(Method method) {
        return JavaMethod.builder()
            .withName(method.getName())
            .withModifiers(getModifiers(method.getModifiers()))
            .withResult(getTypeUse(method.getGenericReturnType()))
            .withGenerics(getTypeUses(method.getTypeParameters()))
            .isVarArgs(method.isVarArgs())
            .isNullable(methodNullableChecker.check(method))
            .withParameters(createParameters(method.getParameters()))
            .withMethodRef(method)
            .build();
    }

    private List<JavaParameter> createParameters(Parameter[] parameters) {
        List<JavaParameter> javaParameters = new ArrayList<>();
        for (Parameter param : parameters) {
            javaParameters.add(JavaParameter.builder()
                .withName(param.getName())
                .withType(getTypeUse(param.getParameterizedType()))
                .isNullable(parameterNullableChecker.check(param))
                .build());
        }
        return javaParameters;
    }

    /**
     * Gets all processed types.
     */
    public List<JavaType> getProcessedTypes() {
        return Collections.unmodifiableList(processedTypes);
    }
}
