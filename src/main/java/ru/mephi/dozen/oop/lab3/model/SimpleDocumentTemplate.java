package ru.mephi.dozen.oop.lab3.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.ToString;

/**
 * Простейшая реализация интерфейса IDocumentTemplate. Объявленные в документе места подстановок рассчитываются лениво,
 * при первом вызове метода {@link SimpleDocumentTemplate#getDefinedBindings}
 */
@ToString
public final class SimpleDocumentTemplate implements IDocumentTemplate {

    @Getter
    private final String source;
    private Set<String> definedBindings = null;

    public SimpleDocumentTemplate(String source) {
        this.source = source;
    }


    private Set<String> getDefinedBindings0() {
        var matcher = VARIABLE_PATTERN.matcher(source);
        var result = new HashSet<String>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<String> getDefinedBindings() {
        if (definedBindings == null) {
            definedBindings = getDefinedBindings0();
        }
        return definedBindings;
    }
}
