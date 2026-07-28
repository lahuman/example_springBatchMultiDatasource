CREATE TABLE IF NOT EXISTS people (
    person_id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    CONSTRAINT uk_people_name UNIQUE (first_name, last_name)
);
