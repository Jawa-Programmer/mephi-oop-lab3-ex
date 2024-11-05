package ru.mephi.dozen.oop.lab3.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import ru.mephi.dozen.oop.lab3.model.IDocumentTemplate;
import ru.mephi.dozen.oop.lab3.model.SimpleDocumentTemplate;
import ru.mephi.dozen.oop.lab3.service.DocumentService;

/**
 * Простейшая реализация сервиса документов, выполняющая все операции синхронно с помощью одного владельца блокировки
 */
public class BlockingDocumentService implements DocumentService {

    private final Map<String, IDocumentTemplate> documents = new HashMap<>();

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


    private Pair<String, String> mapPlaceholders(Pair<String, IDocumentTemplate> entry) {
        if (entry.right == null) {
            return new Pair<>((entry.left), "#{" + entry.left + "}");
        }
        return new Pair<>(entry.left, entry.right.getSource());
    }


    private IDocumentTemplate prepareCompositeTemplate0(String name) {
        var document = documents.get(name);
        if (document == null) {
            return null;
        }
        var data = document.getDefinedBindings().stream()
                .map(it -> new Pair<>(it, prepareCompositeTemplate0(it)))
                .map(this::mapPlaceholders)
                .collect(Collectors.toUnmodifiableMap(Pair::left, Pair::right));
        return new SimpleDocumentTemplate(document.fill(data));
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

    private record Pair<L, R>(L left, R right) {

    }
}
