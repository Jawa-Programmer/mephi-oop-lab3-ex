package ru.mephi.dozen.oop.lab3.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.mephi.dozen.oop.lab3.model.IDocumentTemplate;
import ru.mephi.dozen.oop.lab3.model.SimpleDocumentTemplate;
import ru.mephi.dozen.oop.lab3.service.DigitalSignatureService;
import ru.mephi.dozen.oop.lab3.service.DocumentService;
import ru.mephi.dozen.oop.lab3.util.Pair;

/**
 * Простейшая реализация сервиса документов, выполняющая все операции синхронно с помощью одного владельца блокировки
 */
@RequiredArgsConstructor
public class BlockingDocumentService implements DocumentService {

    private final Map<String, IDocumentTemplate> documents = new HashMap<>();
    private final DigitalSignatureService digitalSignatureService;

    private void checkCycles(String startName, Set<String> dependencies, Set<String> checked) {
        if (dependencies.contains(startName)) {
            throw new IllegalStateException("Cycled dependency for " + startName);
        }
        dependencies.stream()
                .filter(checked::add)
                .map(documents::get)
                .filter(Objects::nonNull)
                .forEach(document -> checkCycles(startName, document.getDefinedBindings(), checked));
    }

    @Override
    public void saveTemplate(String name, IDocumentTemplate template) throws IllegalStateException {
        synchronized (documents) {
            checkCycles(name, template.getDefinedBindings(), new HashSet<>());
            documents.put(name, template);
        }
    }

    @Override
    public Optional<IDocumentTemplate> getTemplate(String name) {
        synchronized (documents) {
            return Optional.ofNullable(documents.get(name));
        }
    }

    @Override
    public void removeTemplate(String name) {
        synchronized (documents) {
            documents.remove(name);
        }
    }


    private IDocumentTemplate prepareCompositeTemplate0(String name) {
        var document = documents.get(name);
        if (document == null) {
            return null;
        }
        // Если документ независимый, возвращаем его
        if (document.getDefinedBindings().isEmpty()) {
            return document;
        }
        var data = document.getDefinedBindings().stream()
                .map(it -> new Pair<>(it, prepareCompositeTemplate0(it))) // рекурсивно собираем зависимости
                .filter(it -> it.right() != null) // опускаем случаи несуществующих документов
                .collect(Collectors.toUnmodifiableMap( // собираем мапу <Имя Документа, Его Подпись>
                        Pair::left,
                        p -> digitalSignatureService.sign(p.right().getSource())
                ));
        // заполняем пропуски цифровыми подписями зависимых документов, формируем из этого новый документ
        return new SimpleDocumentTemplate(document.fillIfPresent(data));
    }

    @Override
    public Optional<IDocumentTemplate> prepareCompositeTemplate(String name) {
        synchronized (documents) {
            return Optional.ofNullable(prepareCompositeTemplate0(name));
        }
    }

    @Override
    public void clean() {
        synchronized (documents) {
            documents.clear();
        }
    }


}
