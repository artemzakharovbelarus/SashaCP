package com.github.artemzakharovbelarus.sasha.pool;

import java.io.Closeable;

/**
 * Interface provides basic operations for connection pool.
 *
 * @param <C> connection class.
 *
 * @author Zakharov Artem.
 */
public interface ConnectionPool<C extends AutoCloseable> extends Closeable {

    /**
     * @return available connection from pool.
     */
    C getConnection();

    /**
     * @param connection connection, that finished it's work.
     */
    void release(C connection);
}
