package ru.mephi.dozen.oop.lab3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import ru.mephi.dozen.oop.lab3.model.IDocumentTemplate;
import ru.mephi.dozen.oop.lab3.model.SimpleDocumentTemplate;
import ru.mephi.dozen.oop.lab3.service.impl.BlockingDocumentService;

public class AbstractTest {

    @SneakyThrows
    protected String readFromSources(String path) {
        var aPath = path.startsWith("/") ? path : '/' + path;
        try (var reader = getClass().getResourceAsStream(aPath)) {
            return new String(reader.readAllBytes());
        }
    }

    private final Random rand = new Random();
    private static final String ALPHABET = "ABCDEFGKLMNPQRSTUVWXYZabcdefgklmnpqrstuvwxyz0123456789_ ";

    private String getLetterOrDep(Set<String> dependencies) {
        if (!dependencies.isEmpty() && rand.nextDouble() < 0.002) {
            var list = new ArrayList<>(dependencies);
            Collections.shuffle(list, rand);
            return "#{" + list.getFirst() + "}";
        }
        var pos = rand.nextInt(ALPHABET.length());
        return "" + ALPHABET.charAt(pos);
    }

    private String randomKey() {
        var key = new StringBuilder();
        var size = rand.nextInt(5, 50);
        for (int i = 0; i < size; ++i) {
            key.append(ALPHABET.charAt(rand.nextInt(ALPHABET.length() - 1)));
        }
        return key.toString();
    }

    protected IDocumentTemplate generateRandomDocument(Set<String> dependencies, int minSize, int maxSize) {
        var size = rand.nextInt(minSize, maxSize);
        StringBuilder bldr = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            bldr.append(getLetterOrDep(dependencies));
        }
        return new SimpleDocumentTemplate(bldr.toString());
    }

    protected Map<String, IDocumentTemplate> generateRandomDocuments(int n, int minSize, int maxSize) {
        Map<String, IDocumentTemplate> documents = new HashMap<>();
        while (documents.size() < n) {
            var next = rand.nextInt(n - documents.size()) + 1;
            for (int i = 0; i < next; ++i) {
                var key = randomKey();
                while (documents.containsKey(key)) {
                    key = randomKey();
                }
                documents.put(key, generateRandomDocument(documents.keySet(), minSize, maxSize));
            }
        }
        return documents;
    }

}

class TestGenerator extends AbstractTest {

    @RepeatedTest(10)
    @DisplayName("Проверка, что генератор документов не создает циклы")
    void testGenerate() {
        final var n = 1000;
        var templates = generateRandomDocuments(n, 500, 10000);
        assertEquals(n, templates.size());
        var service = new BlockingDocumentService();
        assertDoesNotThrow(() -> templates.forEach(service::saveTemplate));
    }
}
