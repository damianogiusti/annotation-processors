package com.damianogiusti.contractannotationprocessor.processor;

import com.damianogiusti.contractannotation.Contract;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static javax.tools.Diagnostic.Kind;

public class ContractProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    /*
     gives you paintbrushes to start painting.
     Filer(to generate file), Messager(debugging), Utility classes.
     You can get these classes with processing environment.
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    /*
        Brain of your processor. Starts rounding and gives you annotated classes, methods, fields, annotation etc.
        It gives you all annotated elements here. And you start doing all calculation and generate your new class file here.
         */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        for (Element element : roundEnvironment.getElementsAnnotatedWith(Contract.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                try {
                    processClass((TypeElement) element);
                } catch (IOException e) {
                    messager.printMessage(Kind.ERROR, e.getLocalizedMessage(), element);
                    return false;
                }
            } else {
                messager.printMessage(Kind.ERROR, "Trying to process a non-Class element", element);
            }
        }
        return false;
    }

    private void processClass(TypeElement typeElement) throws IOException {
        String packageName = getPackageName(typeElement);
        Set<String> fieldNames = new HashSet<>();
        Set<FieldSpec> fields = new LinkedHashSet<>();

        String className = String.format("%s%s", typeElement.getSimpleName().toString(), "Contract");
        ClassName contractClassType = ClassName.get(packageName, className);

        List<? extends Element> elements = typeElement.getEnclosedElements();
        if (elements.size() == 0) {
            // no need to proceed
            return;
        }

        // get field names
        for (Element rawField : elements) {
            if (rawField.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) rawField;
                fieldNames.add(field.getSimpleName().toString());
            }
        }

        // create actual fields
        for (String fieldName : fieldNames) {
            fields.add(FieldSpec.builder(String.class, processFieldName(fieldName), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", fieldName)
                    .build());
        }

        // build the class
        TypeSpec contractClass = TypeSpec.classBuilder(contractClassType)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fields)
                .build();

        // write the class
        JavaFile.builder(contractClassType.packageName(), contractClass)
                .build()
                .writeTo(filer);

    }

    private String processFieldName(String fieldName) {
        if (fieldName.length() == 0) {
            return "";
        }
        char currentChar = fieldName.charAt(0);
        if (Character.isUpperCase(currentChar)) {
            return "_" + Character.toString(currentChar) + processFieldName(fieldName.substring(1));
        } else {
            return Character.toString(currentChar).toUpperCase() + processFieldName(fieldName.substring(1));
        }
    }

    /*
    We return only our custom annotation set in this method.
    We can say that return value of this method will be given to us as process method’s first parameter.
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Contract.class.getCanonicalName());
        return annotations;
    }

    /*
    We always return latest java version.
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private String getPackageName(Element element) {
        return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }
}
