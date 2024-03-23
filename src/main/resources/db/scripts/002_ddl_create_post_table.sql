create table post (
    id      SERIAL PRIMARY KEY,
    "name"    VARCHAR(128),
    "text"    TEXT,
    link    VARCHAR(256) UNIQUE,
    created TIMESTAMP
);