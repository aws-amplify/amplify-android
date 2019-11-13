package com.amplifyframework.datastore.storage.sqlite;

import java.util.Set;

/**
 * Encapsulate the CREATE TABLE and CREATE INDEX commands.
 */
final class CreateSqlCommands {
    private final Set<SqlCommand> createTableCommands;
    private final Set<SqlCommand> createIndexCommands;

    CreateSqlCommands(Set<SqlCommand> createTableCommands,
                             Set<SqlCommand> createIndexCommands) {
        this.createTableCommands = createTableCommands;
        this.createIndexCommands = createIndexCommands;
    }

    Set<SqlCommand> getCreateTableCommands() {
        return createTableCommands;
    }

    Set<SqlCommand> getCreateIndexCommands() {
        return createIndexCommands;
    }
}
