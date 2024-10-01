package org.tera201;

@FunctionalInterface
public interface ThrowConsumer<T> {
    void accept(T t) throws Exception;
}
