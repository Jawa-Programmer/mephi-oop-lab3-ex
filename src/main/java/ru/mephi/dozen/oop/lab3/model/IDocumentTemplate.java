package ru.mephi.dozen.oop.lab3.model;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Интерфейс шаблонного документа. В документе есть местоуказатели формата #{name}, которые можно заполнить данными
 */
public interface IDocumentTemplate {

    Pattern VARIABLE_PATTERN = Pattern.compile("#\\{(\\w*)\\}");

    /**
     * Получить исходный шаблон
     *
     * @return исходный шаблон
     */
    String getSource();

    default Set<String> getDefinedBindings() {
        var matcher = VARIABLE_PATTERN.matcher(getSource());
        var result = new HashSet<String>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    /**
     * Возвращает результат подстановки данных из data в шаблон на позиции #{name}
     *
     * @param data данные
     * @return результирующая строка
     * @throws NoSuchElementException если в data отсутствует поле, которое есть в шаблоне
     */
    default String fill(Map<String, ?> data) throws NoSuchElementException {
        var matcher = VARIABLE_PATTERN.matcher(getSource());
        return matcher.replaceAll(it -> {
            var ret = data.get(it.group(1));
            if (ret == null) {
                throw new NoSuchElementException("Key '" + it.group(1) + "' not found");
            }
            return ret.toString();
        });
    }
}
