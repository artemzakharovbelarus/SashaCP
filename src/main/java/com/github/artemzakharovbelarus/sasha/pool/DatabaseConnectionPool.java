package com.github.artemzakharovbelarus.sasha.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Implementation of connection pool for databases.
 *
 * @author Zakharov Artem.
 */
public class DatabaseConnectionPool implements ConnectionPool<DatabasePooledConnection> {

    private static final String GETTING_CONNECTION_EXCEPTION_MESSAGE = "Exception during getting connection";
    private static final String DRIVER_INIT_EXCEPTION_MESSAGE = "Exception during driver initialization";
    private static final String CREATING_CONNECTION_EXCEPTION_MESSAGE = "Exception during creating " + Connection.class;

    private final SashaConfig config;

    private BlockingQueue<DatabasePooledConnection> freeConnections;
    private BlockingQueue<DatabasePooledConnection> usedConnections;

    /**
     * Initializes all fields, driver and fill queues with connections.
     *
     * @param config configuration.
     */
    public DatabaseConnectionPool(SashaConfig config) {
        this.config = config;

        initDriver();
        initBlockingQueues();
        fillBlockingQueues();
    }

    @Override
    public DatabasePooledConnection getConnection() {
        try {
            return getPooledConnectionFromBlockingQueue();
        } catch (InterruptedException e) {
            throw new IllegalStateException(GETTING_CONNECTION_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public void release(DatabasePooledConnection connection) {
        usedConnections.remove(connection);
        freeConnections.add(connection);
    }

    @Override
    public void close() {
        freeConnections.forEach(DatabasePooledConnection::close);
        usedConnections.forEach(DatabasePooledConnection::close);
    }

    private void initBlockingQueues() {
        freeConnections = new ArrayBlockingQueue<>(config.getPoolSize());
        usedConnections = new ArrayBlockingQueue<>(config.getPoolSize());
    }

    private void initDriver() {
        try {
            Class.forName(config.getDriver());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(DRIVER_INIT_EXCEPTION_MESSAGE, e);
        }
    }

    private void fillBlockingQueues() {
        for (int connectionIndex = 0; connectionIndex < freeConnections.size(); connectionIndex++) {
            DatabasePooledConnection pooledConnection = createDatabasePooledConnection();
            freeConnections.add(pooledConnection);
        }
    }

    private DatabasePooledConnection createDatabasePooledConnection() {
        Connection connection = createConnection();
        return new DatabasePooledConnection(connection, this);
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
        } catch (SQLException e) {
            freeConnections.forEach(DatabasePooledConnection::close);
            throw new IllegalStateException(CREATING_CONNECTION_EXCEPTION_MESSAGE, e);
        }
    }

    private DatabasePooledConnection getPooledConnectionFromBlockingQueue() throws InterruptedException {
        DatabasePooledConnection connection = freeConnections.take();
        usedConnections.add(connection);

        return connection;
    }
}
