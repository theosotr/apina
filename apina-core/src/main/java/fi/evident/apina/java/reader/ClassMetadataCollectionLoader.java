package fi.evident.apina.java.reader;

import fi.evident.apina.java.model.ClassMetadataCollection;
import fi.evident.apina.java.model.JavaClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ClassMetadataCollectionLoader {

    private ClassMetadataCollectionLoader() { }

    public static ClassMetadataCollection load(Classpath classpath) throws IOException {
        List<JavaClass> classes = new ArrayList<>();

        ClassPathScanner.processAllClasses(classpath, in -> {
            classes.add(ClassMetadataReader.loadMetadata(in));
        });

        return new ClassMetadataCollection(classes);
    }
}