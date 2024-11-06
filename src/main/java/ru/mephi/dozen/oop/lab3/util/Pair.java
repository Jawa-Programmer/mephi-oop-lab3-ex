package ru.mephi.dozen.oop.lab3.util;

/**
 * Пара значений
 *
 * @param left первое значение
 * @param right второе значение
 * @param <L> тип первого значения
 * @param <R> тип второго значения
 */
public record Pair<L, R>(L left, R right) {

}