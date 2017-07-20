package com.damianogiusti.mapper.processor;

import com.damianogiusti.mappable.Mappable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public class MapperProcessor extends AbstractProcessor {

    public static final String PACKAGE_NAME = "com.damianogiusti.mapper";
    public static final String CLASS_NAME = "ObjectMapFactory";
    public static final String DEFAULT_OBJ_NAME = "obj";
    public static final String DEFAULT_MAP_NAME = "map";

    private Messager messager;
    private Set<TypeElement> elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        elements = new LinkedHashSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        for (Element element : roundEnvironment.getElementsAnnotatedWith(Mappable.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                try {
                    TypeElement typeElement = (TypeElement) element;
                    if (!elements.contains(typeElement)) {
                        methodSpecs.addAll(processClass(typeElement));
                        elements.add(typeElement);
                    }
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, e.getLocalizedMessage(), element);
                    return false;
                }
            } else {
                messager.printMessage(Diagnostic.Kind.ERROR, "Trying to process a non-Class element", element);
            }
        }

        // build the class
        TypeSpec contractClass = TypeSpec.classBuilder(CLASS_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethods(methodSpecs)
                .build();

        // write the class
        try {
            JavaFile.builder(PACKAGE_NAME, contractClass)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException ignored) {
        }
        return false;
    }

    private List<MethodSpec> processClass(TypeElement typeElement) throws IOException {
        String classNameString = typeElement.getSimpleName().toString();
        ClassName className = ClassName.get(typeElement);
        Map<String, TypeMirror> fieldNames = new HashMap<>();
        Map<ExecutableElement, String> getterNames = new HashMap<>();
        Map<ExecutableElement, String> setterNames = new HashMap<>();

        List<? extends Element> elements = typeElement.getEnclosedElements();
        if (elements.size() == 0) {
            // no need to proceed
            return Collections.emptyList();
        }

        // get field names
        for (Element rawField : elements) {
            if (rawField.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) rawField;
                // map: [ fieldName : type ]
                fieldNames.put(field.getSimpleName().toString(), field.asType());
            }
        }

        for (Element rawMethod : elements) {
            if (rawMethod.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) rawMethod;

                String returnedFieldName = getter(method, fieldNames.keySet());
                // if method is getter of a field, insert into collection
                if (returnedFieldName != null) {
                    getterNames.put(method, returnedFieldName);
                } else {
                    String setFieldName = setter(method, fieldNames.keySet());
                    // if method is setter of a field, insert into collection
                    if (setFieldName != null) {
                        setterNames.put(method, setFieldName);
                    }
                }
            }
        }

        // --- TO method

        MethodSpec.Builder toMethodBuilder = MethodSpec.methodBuilder(String.format("%sToMap", classNameString.toLowerCase()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .returns(ParameterizedTypeName.get(Map.class, String.class, Object.class))
                .addParameter(ParameterSpec.builder(className, DEFAULT_OBJ_NAME).build())
                .addStatement("$T<String, Object> $L = new $T<>()", Map.class, DEFAULT_MAP_NAME, HashMap.class);


        // put fields into map
        for (ExecutableElement method : getterNames.keySet()) {
            String methodName = name(method);


            TypeMirror returnType = method.getReturnType();
            if (returnType.getKind() == TypeKind.DECLARED && isDate((TypeElement) processingEnv.getTypeUtils().asElement(returnType))) {
                // add the Date as long if not null
                toMethodBuilder = toMethodBuilder
                        .beginControlFlow("if ($L.$L() != null)", DEFAULT_OBJ_NAME, methodName)
                        .addStatement("$L.put($S, $L.$L().getTime())", DEFAULT_MAP_NAME, getterNames.get(method), DEFAULT_OBJ_NAME, methodName)
                        .endControlFlow();
            } else {
                toMethodBuilder = toMethodBuilder.addStatement("$L.put($S, $L.$L())",
                        DEFAULT_MAP_NAME, getterNames.get(method), DEFAULT_OBJ_NAME, methodName);
            }
        }

        toMethodBuilder.addStatement("return $L", DEFAULT_MAP_NAME);

        // --- FROM method

        MethodSpec.Builder fromMethodBuilder = MethodSpec.methodBuilder(String.format("%sFromMap", classNameString.toLowerCase()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .returns(className)
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Object.class), DEFAULT_MAP_NAME).build())
                .addStatement("$T $L = new $T()", className, DEFAULT_OBJ_NAME, className);

        // get fields from map
        for (ExecutableElement method : setterNames.keySet()) {
            String methodName = name(method);
            String currentFieldName = setterNames.get(method);
            TypeMirror currentFieldType = fieldNames.get(currentFieldName);
            if (currentFieldType.getKind() == TypeKind.DECLARED && isDate((TypeElement) processingEnv.getTypeUtils().asElement(currentFieldType))) {
                // do a null check and if not null then instantiate a Date
                fromMethodBuilder = fromMethodBuilder.addStatement("$L.$L($L.get($S) != null ? new $T(($T) $L.get($S)) : null)",
                        DEFAULT_OBJ_NAME, methodName, DEFAULT_MAP_NAME, currentFieldName, Date.class, long.class, DEFAULT_MAP_NAME, currentFieldName);
            } else {
                // simply cast the object
                fromMethodBuilder = fromMethodBuilder.addStatement("$L.$L(($T) $L.get($S))",
                        DEFAULT_OBJ_NAME, methodName, currentFieldType, DEFAULT_MAP_NAME, currentFieldName);
            }
        }

        fromMethodBuilder.addStatement("return $L", DEFAULT_OBJ_NAME);

        return Arrays.asList(toMethodBuilder.build(), fromMethodBuilder.build());
    }

    private String setter(ExecutableElement method, Iterable<String> fields) {
        String methodName = method.getSimpleName().toString().toLowerCase();
        if (!methodName.contains("set")) {
            return null;
        }
        for (String fieldName : fields) {
            if (methodName.contains(fieldName.toLowerCase())) {
                return fieldName;
            }
        }
        return null;
    }

    private String getter(final ExecutableElement method, final Iterable<String> fields) {
        String methodName = method.getSimpleName().toString().toLowerCase();
        if (!methodName.contains("get")) {
            return null;
        }
        for (String fieldName : fields) {
            if (methodName.contains(fieldName.toLowerCase())) {
                return fieldName;
            }
        }
        return null;
    }

    private boolean isDate(TypeElement element) {
        return element.getSimpleName().toString().equals(Date.class.getSimpleName());
    }

    private String name(Element method) {
        return method.getSimpleName().toString();
    }

    private String getPackageName(Element element) {
        return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Mappable.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
