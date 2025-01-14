/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.github.emmmm9o.javatypes.JavaTypes.*;
import com.github.emmmm9o.javatypes.exceptions.TypeGenerationException;

/**
 * Generator for converting Java types to TypeScript declaration files.
 * This class handles the conversion of Java types into TypeScript type definitions,
 * including proper handling of generics, interfaces, and class hierarchies.
 */
public class TSGenerator implements Generator {
    private final Map<String, String> typeReferenceMap;
    private final Map<String, Boolean> noGenericsMap;
    private final Map<String, String> customTypeMap;
    private final Map<String, String> additionalTypeMap;
    private final Map<String, TSModule> moduleReferenceMap;
    private final TSModule rootModule;
    private String modulePrefix = "";

    /**
     * Interface for providing additional type information.
     */
    @FunctionalInterface
    public interface TypeInfoProvider<T> {
        String getInfo(T item);
    }

    private final TypeInfoProvider<JavaMethod> methodInfoProvider;
    private final TypeInfoProvider<JavaMethod> constructorInfoProvider;
    private final TypeInfoProvider<JavaField> fieldInfoProvider;
    private final TypeInfoProvider<JavaType> classInfoProvider;

    /**
     * Creates a new TypeScript generator with default settings.
     */
    public TSGenerator() {
        this(m -> "", m -> "", f -> "", c -> "");
    }

    /**
     * Creates a new TypeScript generator with custom info providers.
     */
    public TSGenerator(
            TypeInfoProvider<JavaMethod> methodInfo,
            TypeInfoProvider<JavaMethod> constructorInfo,
            TypeInfoProvider<JavaField> fieldInfo,
            TypeInfoProvider<JavaType> classInfo) {
        this.methodInfoProvider = methodInfo;
        this.constructorInfoProvider = constructorInfo;
        this.fieldInfoProvider = fieldInfo;
        this.classInfoProvider = classInfo;
        this.typeReferenceMap = new ConcurrentHashMap<>();
        this.noGenericsMap = new ConcurrentHashMap<>();
        this.customTypeMap = new ConcurrentHashMap<>();
        this.additionalTypeMap = new ConcurrentHashMap<>();
        this.moduleReferenceMap = new ConcurrentHashMap<>();
        this.rootModule = new TSModule();
        initializeDefaultTypeMappings();
    }

    /**
     * Represents a TypeScript module.
     */
    public static class TSModule {
        private final Map<String, TSModule> modules;
        private final Map<String, String> types;
        private final String name;

        public TSModule() {
            this("", new HashMap<>(), new HashMap<>());
        }

        public TSModule(String name) {
            this(name, new HashMap<>(), new HashMap<>());
        }

        private TSModule(String name, Map<String, TSModule> modules, Map<String, String> types) {
            this.name = name;
            this.modules = modules;
            this.types = types;
        }

        public Map<String, TSModule> getModules() {
            return Collections.unmodifiableMap(modules);
        }

        public Map<String, String> getTypes() {
            return Collections.unmodifiableMap(types);
        }

        public String getName() {
            return name;
        }
    }

    private void initializeDefaultTypeMappings() {
        // Primitive type mappings
        addTypeMapping("int", "number", true);
        addTypeMapping("float", "number", true);
        addTypeMapping("double", "number", true);
        addTypeMapping("void", "void", true);
        addTypeMapping("boolean", "boolean", true);
        addTypeMapping("char", "string", true);
        addTypeMapping("byte", "string", true);

        // Common Java class mappings
        addTypeMapping("java.lang.String", "string");
        addTypeMapping("java.lang.Double", "number");
        addTypeMapping("java.lang.Float", "number");
        addTypeMapping("java.lang.Integer", "number");
        addTypeMapping("java.lang.Void", "void");
        addTypeMapping("java.lang.Boolean", "boolean");
        addTypeMapping("java.lang.Object", "any");
        addTypeMapping("java.lang.Character", "string");
        addTypeMapping("java.lang.Runnable", "()=>void");

        // Add custom type declarations
        addCustomTypeDeclaration("java.lang.Runnable", "declare type Runnable=()=>void");
    }

    /**
     * Adds a type mapping from Java to TypeScript.
     */
    public void addTypeMapping(String javaType, String tsType) {
        addTypeMapping(javaType, tsType, false);
    }

    /**
     * Adds a type mapping from Java to TypeScript with control over generics handling.
     */
    public void addTypeMapping(String javaType, String tsType, boolean noGenerics) {
        typeReferenceMap.put(javaType, tsType);
        if (noGenerics) {
            noGenericsMap.put(javaType, true);
        }
    }

    /**
     * Adds a custom type declaration.
     */
    public void addCustomTypeDeclaration(String type, String declaration) {
        customTypeMap.put(type, declaration);
    }

    /**
     * Gets the module path for a type.
     */
    public String getModulePath(JavaType type) {
        String path = type.getClasspath().replace("$", ".");
        int lastDot = path.lastIndexOf(".");
        return lastDot > 0 ? path.substring(0, lastDot) : "";
    }

    /**
     * Gets or creates a module for the given path.
     */
    public TSModule getModule(String modulePath) {
        return moduleReferenceMap.computeIfAbsent(modulePath, path -> {
            TSModule current = rootModule;
            if (!path.isEmpty()) {
                for (String part : path.replace("function", "_function").split("\\.")) {
                    current = current.modules.computeIfAbsent(part, TSModule::new);
                }
            }
            return current;
        });
    }

    /**
     * Initializes modules for all types.
     */
    public void initializeModules(Map<String, JavaType> typeMap) {
        moduleReferenceMap.put("", rootModule);
        typeMap.values().forEach(type -> {
            String modulePath = getModulePath(type);
            getModule(modulePath).types.put(type.getName(), type.getClasspath());
        });
    }

    /**
     * Generates TypeScript type for a Java type.
     */
    public String generateType(JavaType type) {
        return generateType(type, false);
    }

    public String generateType(JavaType type, boolean useUpperBound) {
        if (!useUpperBound) {
            String tsType = typeReferenceMap.get(type.getClasspath());
            if (tsType != null) {
                return tsType;
            }
        }
        return (modulePrefix + type.getClasspath())
            .replace("$", ".")
            .replace("function", "_function")
            .replace(".type", "._type")
            .replaceAll("\\.\\d+", ".");
    }

    /**
     * Generates TypeScript generics declaration.
     */
    public String generateGenerics(List<JavaTypeUse> generics) {
        return generateGenerics(generics, false);
    }

    public String generateGenerics(List<JavaTypeUse> generics, boolean useUpperBound) {
        if (generics == null || generics.isEmpty()) {
            return "";
        }

        return "<" + generics.stream()
            .map(type -> generateTypeUse(type, useUpperBound))
            .collect(Collectors.joining(",")) + ">";
    }

    /**
     * Generates TypeScript type for a Java type use.
     */
    public String generateTypeUse(JavaTypeUse typeUse) {
        return generateTypeUse(typeUse, false);
    }

    public String generateTypeUse(JavaTypeUse typeUse, boolean useUpperBound) {
        StringBuilder builder = new StringBuilder();

        if ("?".equals(typeUse.getTypeGeneric())) {
            builder.append("any");
        } else if (typeUse.getTypeGeneric() != null) {
            builder.append(typeUse.getTypeGeneric());
        }

        if (typeUse.getComponentType() != null) {
            builder.append(generateTypeUse(typeUse.getComponentType(), false))
                .append("[]");
        }

        if (typeUse.getType() != null) {
            builder.append(generateType(typeUse.getType()));
            
            // Handle generic parameters
            if (typeUse.getGenerics().isEmpty() && !typeUse.getType().getGenerics().isEmpty()) {
                builder.append("<")
                    .append(String.join(",", Collections.nCopies(typeUse.getType().getGenerics().size(), "any")))
                    .append(">");
            } else {
                builder.append(generateGenerics(typeUse.getGenerics()));
            }
        }

        // Handle bounds
        if (typeUse.getUpperBounds() != null && !typeUse.getUpperBounds().isEmpty() &&
            (useUpperBound || typeUse.getTypeGeneric() == null)) {
            List<JavaTypeUse> bounds = typeUse.getUpperBounds().stream()
                .filter(bound -> bound.getType() == null || 
                    !bound.getType().getClasspath().equals("java.lang.Object"))
                .collect(Collectors.toList());

            if (!bounds.isEmpty()) {
                builder.append(" extends ")
                    .append(bounds.stream()
                        .map(bound -> generateTypeUse(bound, false))
                        .collect(Collectors.joining("&")));
            }
        }

        return builder.toString();
    }

    /**
     * Generates TypeScript modifier.
     */
    public String generateModifier(JavaModifier modifier) {
        if (modifier == JavaModifier.Final || modifier == JavaModifier.Abstract) {
            return "";
        }
        return modifier.toString().toLowerCase();
    }

    /**
     * Generates TypeScript modifiers.
     */
    public String generateModifiers(Set<JavaModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "";
        }
        return modifiers.stream()
            .map(this::generateModifier)
            .filter(mod -> !mod.isEmpty())
            .collect(Collectors.joining(" ")) + " ";
    }

    /**
     * Generates TypeScript field declaration.
     */
    public String generateField(JavaField field) {
        StringBuilder builder = new StringBuilder();
        String info = fieldInfoProvider.getInfo(field);
        if (!info.isEmpty()) {
            builder.append("/*").append(info).append("*/\n");
        }

        builder.append(generateModifiers(field.getModifiers()))
            .append(field.getName().replace("constructor", "_constructor"))
            .append(field.isNullable() ? "?:" : ":")
            .append(generateTypeUse(field.getType()))
            .append(";");

        return builder.toString();
    }

    /**
     * Generates TypeScript parameter declaration.
     */
    public String generateParameter(JavaParameter parameter, boolean isVarArgs) {
        StringBuilder builder = new StringBuilder();
        builder.append("_").append(parameter.getName())
            .append(":")
            .append(generateParameterType(parameter));

        if (parameter.isNullable()) {
            builder.append(" | null");
        }

        if (isVarArgs) {
            builder.append("[]");
        }

        return builder.toString();
    }

    private String generateParameterType(JavaParameter parameter) {
        if (parameter.getType().getType() != null && 
            parameter.getType().getType().getClasspath().equals("java.lang.Class")) {
            StringBuilder builder = new StringBuilder("Class");
            if (!parameter.getType().getGenerics().isEmpty()) {
                builder.append("<")
                    .append(generateTypeUse(parameter.getType().getGenerics().get(0)))
                    .append(">");
            }
            builder.append(" | ");
            return builder.toString();
        }
        return generateTypeUse(parameter.getType());
    }

    /**
     * Generates TypeScript method declaration.
     */
    public String generateMethod(JavaMethod method) {
        StringBuilder builder = new StringBuilder();
        String info = method.getConstructorRef() != null ? 
            constructorInfoProvider.getInfo(method) : 
            methodInfoProvider.getInfo(method);

        if (!info.isEmpty()) {
            builder.append("/*").append(info).append("*/\n");
        }

        builder.append(generateModifiers(method.getModifiers()));

        if (method.getName() != null) {
            builder.append(method.getName());
        }

        builder.append(generateGenerics(method.getGenerics(), true))
            .append("(");

        // Generate parameters
        List<JavaParameter> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(generateParameter(parameters.get(i), 
                method.isVarArgs() && i == parameters.size() - 1));
        }

        builder.append(")");

        // Add return type for methods
        if (method.getResult() != null) {
            builder.append(":")
                .append(generateTypeUse(method.getResult()))
                .append(method.isNullable() ? " | null" : "");
        }

        builder.append(";");
        return builder.toString();
    }

    /**
     * Generates complete TypeScript declaration file content.
     */
    public String generateDeclarationFile(JavaType type) {
        if (type == null) {
            throw new TypeGenerationException("Cannot generate declaration file for null type");
        }

        try {
            StringBuilder builder = new StringBuilder();
            String info = classInfoProvider.getInfo(type);
            
            if (!info.isEmpty()) {
                builder.append("/*").append(info).append("*/\n");
            }

            // Check if this type should be skipped
            if (noGenericsMap.getOrDefault(type.getClasspath(), false)) {
                return "";
            }

            // Add any custom type declarations first
            String customDeclaration = customTypeMap.get(type.getClasspath());
            if (customDeclaration != null) {
                builder.append(customDeclaration).append("\n\n");
            }

            // Generate class/interface declaration
            builder.append(generateModifiers(type.getModifiers()))
                .append("declare ")
                .append(type.getModifiers().contains(JavaModifier.Abstract) ? "abstract " : "")
                .append(type.getClassRef().isInterface() ? "interface " : "class ")
                .append(type.getName())
                .append(generateGenerics(type.getGenerics(), true));

            // Handle inheritance
            if (type.getSuperType() != null && 
                !type.getSuperType().getType().getClasspath().equals("java.lang.Object")) {
                builder.append(" extends ")
                    .append(generateTypeUse(type.getSuperType()));
            }

            // Handle interfaces
            Set<JavaTypeUse> interfaces = type.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                builder.append(type.getClassRef().isInterface() ? " extends " : " implements ")
                    .append(interfaces.stream()
                        .map(this::generateTypeUse)
                        .collect(Collectors.joining(", ")));
            }

            builder.append(" {\n");

            // Generate members
            type.getFields().forEach(field -> 
                addSpaces(generateField(field), builder));
            
            if (!type.getClassRef().isInterface()) {
                type.getConstructors().forEach(constructor -> 
                    addSpaces(generateMethod(constructor), builder));
            }
            
            type.getMethods().forEach(method -> 
                addSpaces(generateMethod(method), builder));

            builder.append("}\n");

            // Generate inner types
            type.getInnerClasses().forEach(innerType -> 
                builder.append("\n").append(generateDeclarationFile(innerType)));

            // Add any additional type declarations
            String additionalDeclaration = additionalTypeMap.get(type.getClasspath());
            if (additionalDeclaration != null) {
                builder.append("\n").append(additionalDeclaration);
            }

            return builder.toString();
        } catch (Exception e) {
            throw new TypeGenerationException("Failed to generate declaration file for type: " + type.getClasspath(), e);
        }
    }

    private void addSpaces(String input, StringBuilder builder) {
        Arrays.stream(input.split("\n"))
            .forEach(line -> builder.append("  ").append(line).append("\n"));
    }

    public void setModulePrefix(String prefix) {
        this.modulePrefix = prefix;
    }

    public TSModule getRootModule() {
        return rootModule;
    }

    /**
     * Adds a type-specific additional declaration that will be appended after the main type declaration.
     */
    public void addAdditionalTypeDeclaration(String type, String declaration) {
        if (type != null && declaration != null) {
            additionalTypeMap.put(type, declaration);
        }
    }

    /**
     * Generates a complete module declaration including all its types.
     */
    public String generateModuleDeclaration(TSModule module, Map<String, JavaType> typeMap) {
        if (module == null || typeMap == null) {
            throw new TypeGenerationException("Module and type map cannot be null");
        }

        StringBuilder builder = new StringBuilder();

        // Generate namespace declaration if needed
        if (!module.getName().isEmpty()) {
            builder.append("declare namespace ").append(module.getName()).append(" {\n");
        }

        // Generate sub-modules
        module.getModules().values().forEach(subModule -> {
            String subModuleDeclaration = generateModuleDeclaration(subModule, typeMap);
            if (!subModuleDeclaration.isEmpty()) {
                if (!module.getName().isEmpty()) {
                    addSpaces(subModuleDeclaration, builder);
                } else {
                    builder.append(subModuleDeclaration);
                }
                builder.append("\n");
            }
        });

        // Generate types
        module.getTypes().values().forEach(typePath -> {
            JavaType type = typeMap.get(typePath);
            if (type != null) {
                String typeDeclaration = generateDeclarationFile(type);
                if (!typeDeclaration.isEmpty()) {
                    if (!module.getName().isEmpty()) {
                        addSpaces(typeDeclaration, builder);
                    } else {
                        builder.append(typeDeclaration);
                    }
                    builder.append("\n");
                }
            }
        });

        if (!module.getName().isEmpty()) {
            builder.append("}\n");
        }

        return builder.toString();
    }
}
