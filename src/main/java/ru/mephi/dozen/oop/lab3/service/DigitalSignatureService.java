package ru.mephi.dozen.oop.lab3.service;

/**
 * Сервис получения цифровой подписи документов
 */
public interface DigitalSignatureService {

    /**
     * Подписывает цифровой документ
     *
     * @param str документ для подписи
     * @return цифровая подпись документа
     */
    String sign(String str);
}
