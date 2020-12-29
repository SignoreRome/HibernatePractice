BEGIN;

DROP TABLE IF EXISTS items CASCADE;
CREATE TABLE items (id serial NOT NULL PRIMARY KEY, val int, version int DEFAULT 0);
INSERT INTO items (val) VALUES
        (0),(0),(0),(0),(0),(0),(0),(0),(0),(0),
        (0),(0),(0),(0),(0),(0),(0),(0),(0),(0),
        (0),(0),(0),(0),(0),(0),(0),(0),(0),(0),
        (0),(0),(0),(0),(0),(0),(0),(0),(0),(0);

COMMIT;