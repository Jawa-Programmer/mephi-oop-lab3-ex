package ru.mephi.dozen.oop.lab3.service;

import java.util.Optional;
import ru.mephi.dozen.oop.lab3.model.IDocumentTemplate;

/**
 * Сервис управления документами. Особенностью является то, что документы связаны между собой древовидной структурой.
 * Для подстановки #{name} используются другие документы из этого же сервиса
 */
public interface DocumentService {

    /**
     * Метод добавляет или обновляет документ по имени
     *
     * @param name имя, которые используют другие документы в #{name}
     * @param template сам документ
     * @throws IllegalStateException если обнаружена циклическая зависимость
     */
    void saveTemplate(String name, IDocumentTemplate template) throws IllegalStateException;

    /**
     * Возвращает документ по его имени
     *
     * @param name имя документа
     * @return шаблон документа
     */
    Optional<IDocumentTemplate> getTemplate(String name);

    /**
     * Удаляет документ по его имени
     *
     * @param name имя документа
     */
    void removeTemplate(String name);

    /**
     * Предзаполняет документ. Подменяет #{name} на содержимое других предзаполненных документов с таким именем. Для
     * отсутствующих в репозитории имён оставляет местоуказатель #{name}
     *
     * @param name имя шаблона документа для предзаполнения
     * @return Документ, в котором
     */
    Optional<IDocumentTemplate> prepareCompositeTemplate(String name);

    /**
     * Удаляет все документы и переводит сервис в начальное состояние
     */
    void clean();
}
