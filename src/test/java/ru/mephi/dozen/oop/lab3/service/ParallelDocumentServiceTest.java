package ru.mephi.dozen.oop.lab3.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import ru.mephi.dozen.oop.lab3.AbstractTest;
import ru.mephi.dozen.oop.lab3.model.IDocumentTemplate;
import ru.mephi.dozen.oop.lab3.model.SimpleDocumentTemplate;
import ru.mephi.dozen.oop.lab3.service.impl.ParallelDocumentService;

class ParallelDocumentServiceTest extends AbstractTest {

    private DocumentService service;

    @BeforeEach
    void init() {
        // TODO: инициализировать ParallelDocumentService (если был определен конструктор с аргументами)
        service = new ParallelDocumentService();
    }

    @Test
    @DisplayName("Вставка новых шаблонов без циклов не приводит к ошибке")
    void saveTemplate() {
        var template1 = new SimpleDocumentTemplate("#{template2}");
        var template2 = new SimpleDocumentTemplate("#{template3}");
        service.saveTemplate("template1", template1);
        service.saveTemplate("template2", template2);

        assertTrue(service.getTemplate("template1").isPresent());
        assertTrue(service.getTemplate("template2").isPresent());
        assertEquals(template1, service.getTemplate("template1").get());
        assertEquals(template2, service.getTemplate("template2").get());
        assertTrue(service.getTemplate("template3").isEmpty());
    }

    @Test
    @DisplayName("Вставка новых шаблонов c глубокими циклами приводит к ошибке")
    void saveTemplate_failOnLongCycle() {
        var template1 = new SimpleDocumentTemplate("#{template2}");
        var template2 = new SimpleDocumentTemplate("#{template3}");
        var template3 = new SimpleDocumentTemplate("#{template1}");
        service.saveTemplate("template1", template1);
        service.saveTemplate("template2", template2);

        var ex = assertThrows(IllegalStateException.class, () -> service.saveTemplate("template3", template3));
        assertEquals("Cycled dependency for template3", ex.getMessage());
    }

    @Test
    @DisplayName("Вставка новых шаблонов c короткими циклами приводит к ошибке")
    void saveTemplate_failOnShortCycle() {
        var template1 = new SimpleDocumentTemplate("#{template2}");
        var template2 = new SimpleDocumentTemplate("#{template1}");
        service.saveTemplate("template1", template1);

        var ex = assertThrows(IllegalStateException.class, () -> service.saveTemplate("template2", template2));
        assertEquals("Cycled dependency for template2", ex.getMessage());
    }

    @Test
    void removeTemplate() {
        assertTrue(service.getTemplate("template1").isEmpty());
        assertTrue(service.getTemplate("template2").isEmpty());
        assertTrue(service.getTemplate("template3").isEmpty());

        var template1 = new SimpleDocumentTemplate("#{template2}");
        var template2 = new SimpleDocumentTemplate("#{template3}");
        var template3 = new SimpleDocumentTemplate("#{template4}");

        service.saveTemplate("template1", template1);
        service.saveTemplate("template2", template2);
        service.saveTemplate("template3", template3);

        assertTrue(service.getTemplate("template1").isPresent());
        assertTrue(service.getTemplate("template2").isPresent());
        assertTrue(service.getTemplate("template3").isPresent());

        service.removeTemplate("template2");

        assertTrue(service.getTemplate("template1").isPresent());
        assertTrue(service.getTemplate("template2").isEmpty());
        assertTrue(service.getTemplate("template3").isPresent());
    }

    @Test
    void prepareCompositeTemplate() {
        var excepted = readFromSources("/actual/contract_edit.txt");

        var toSave = List.of("company_address", "contract", "contract_edit", "user_address", "user_credit");
        var prefix = "/templates/";
        toSave.stream()
                .map(it -> Map.entry(it, readFromSources(prefix + it + ".txt")))
                .forEach(e -> service.saveTemplate(e.getKey(), new SimpleDocumentTemplate(e.getValue())));

        var result = service.prepareCompositeTemplate("contract_edit");

        assertEquals(excepted, result.map(IDocumentTemplate::getSource).orElse(null));
    }

    private final Random random = new Random();

    private void saveDocument(String doc, IDocumentTemplate template) {
        service.saveTemplate(doc, template);
    }

    private void removeDocument(String doc) {
        service.removeTemplate(doc);
    }

    private Optional<IDocumentTemplate> getDocument(String doc) {
        return service.getTemplate(doc);
    }

    private Optional<IDocumentTemplate> requestDocument(String doc) {
        return service.prepareCompositeTemplate(doc);
    }

    private <T> T randomElement(Collection<T> collection) {
        var list = List.copyOf(collection);
        return list.get(random.nextInt(list.size()));
    }

    @SneakyThrows
    private <T> T getTask(Future<T> future) {
        return future.get(2, TimeUnit.SECONDS);
    }

    private Callable<String> timed(String name, Runnable runnable) {
        return () -> {
            try {
                Thread.sleep(random.nextLong(50));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            long start = System.currentTimeMillis();
            runnable.run();
            long end = System.currentTimeMillis();
            return "Elapsed time for " + name + ": " + (end - start);
        };
    }

    private void printAverageByPrefix(String prefix, List<String> msg) {
        var begin = prefix.length();
        msg.stream()
                .filter(s -> s.startsWith(prefix))
                .mapToDouble(s -> Double.parseDouble(s.substring(begin)))
                .average()
                .ifPresent(val -> System.out.println(prefix + val));
    }

    @RepeatedTest(100)
    @Disabled("Тест бенчмарк, работает долго. Запустить для проверки многопоточности")
    @DisplayName("Проверка, что выполнение в многопоточной среде не приводит к возникновениям ошибок")
    void multithreadingTest() {
        try (ExecutorService pool = new ThreadPoolExecutor(4, 10,
                15, TimeUnit.SECONDS, new LinkedBlockingQueue<>())) {
            final var tasksCount = 100;

            var documents = generateRandomDocuments(200, 500, 10000);
            documents.forEach((k, v) -> service.saveTemplate(k, v));
            List<Future<String>> tasks = new ArrayList<>();

            for (int i = 0; i < tasksCount; ++i) {
                var randKey = randomElement(documents.keySet());
                var task = switch (random.nextInt(5)) {
                    case 0 -> pool.submit(timed("Save", () -> saveDocument(randKey, documents.get(randKey))));
                    case 1 -> pool.submit(timed("Remove", () -> removeDocument(randKey)));
                    case 2 -> pool.submit(timed("Get", () -> getDocument(randKey)));
                    default -> pool.submit(timed("Prepare", () -> requestDocument(randKey)));
                };
                tasks.add(task);
            }
            var results = tasks.stream()
                    .map(this::getTask)
                    .toList();
            System.out.println("========================================");
            printAverageByPrefix("Elapsed time for Save: ", results);
            printAverageByPrefix("Elapsed time for Remove: ", results);
            printAverageByPrefix("Elapsed time for Get: ", results);
            printAverageByPrefix("Elapsed time for Prepare: ", results);

            assertEquals(tasksCount, results.size());
        }
    }

    @Test
    void clean() {
        assertTrue(service.getTemplate("template1").isEmpty());
        assertTrue(service.getTemplate("template2").isEmpty());

        var template1 = new SimpleDocumentTemplate("#{template2}");
        var template2 = new SimpleDocumentTemplate("#{template3}");
        service.saveTemplate("template1", template1);
        service.saveTemplate("template2", template2);

        assertTrue(service.getTemplate("template1").isPresent());
        assertTrue(service.getTemplate("template2").isPresent());

        service.clean();

        assertTrue(service.getTemplate("template1").isEmpty());
        assertTrue(service.getTemplate("template2").isEmpty());
    }
}