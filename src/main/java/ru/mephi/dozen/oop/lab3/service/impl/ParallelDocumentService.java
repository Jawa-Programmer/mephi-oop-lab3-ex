package ru.mephi.dozen.oop.lab3.service.impl;

import java.util.NoSuchElementException;
import java.util.Optional;
import ru.mephi.dozen.oop.lab3.model.IDocumentTemplate;
import ru.mephi.dozen.oop.lab3.service.DocumentService;

/**
 * Реализация сервиса документов, оптимизированная для доступа из разных потоков и выполняющая сборку шаблона с помощью
 * ForkJoin процесса
 */
public class ParallelDocumentService implements DocumentService {
    // TODO: собственно, написать реализацию :D

    @Override
    public void saveTemplate(String name, IDocumentTemplate template) throws IllegalStateException {

    }

    @Override
    public Optional<IDocumentTemplate> getTemplate(String name) {
        return Optional.empty();
    }

    @Override
    public void removeTemplate(String name) {

    }

    @Override
    public Optional<IDocumentTemplate> prepareCompositeTemplate(String name) throws NoSuchElementException {
        return Optional.empty();
    }

    @Override
    public void clean() {

    }
}
