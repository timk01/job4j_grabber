create table post (
    id      SERIAL PRIMARY KEY,
    "name"    VARCHAR(128),
    link    VARCHAR(256) UNIQUE,
    "text"    TEXT,
    created TIMESTAMP
);