package ru.job4j.grabber;

public enum PsqlStoreStatements {
    SAVE("INSERT INTO post(name, link, text, created) VALUES (?, ?, ?, ?) ON CONFLICT(link) DO NOTHING"),
    GET_ALL("SELECT * FROM post"),
    FIND_BY_ID("SELECT * FROM post where id = ?");

    private String statement;

    PsqlStoreStatements(String statement) {
        this.statement = statement;
    }

    public String getStatement() {
        return statement;
    }
}
