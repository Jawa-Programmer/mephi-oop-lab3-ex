package ru.mephi.dozen.oop.lab3.service;

import java.util.Optional;
import ru.mephi.dozen.oop.lab3.model.IDocumentTemplate;

/**
 * Сервис управления документами. Особенностью является то, что документы связаны между собой древовидной структурой.
 * В документах может присутствовать ссылка на другие документы (формат #{name}), которая подменяется цифровой подписью документа
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
     * Возвращает шаблон документа по его имени
     *
     * @param name имя документа
     * @return шаблон документа
     */
    Optional<IDocumentTemplate> getTemplate(String name);

    /**
     * Удаляет шаблон документ по его имени
     *
     * @param name имя документа
     */
    void removeTemplate(String name);

    /**
     * Предзаполняет документ. Подменяет #{name} на цифровые подписи документов с соответствующим именем.
     * Подпись с документа-зависимости берется только после того, как в него подставлены подписи тех документов,
     * от которых зависит он сам
     *
     * @param name имя шаблона документа для предзаполнения
     * @return Документ, в котором местоуказатели заполнены цифровыми подписями
     */
    Optional<IDocumentTemplate> prepareCompositeTemplate(String name);

    /**
     * Удаляет все документы и переводит сервис в начальное состояние
     */
    void clean();
}
