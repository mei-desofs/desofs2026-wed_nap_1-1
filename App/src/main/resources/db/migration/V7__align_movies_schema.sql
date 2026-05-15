ALTER TABLE movies
    ADD COLUMN genre VARCHAR(100) NOT NULL DEFAULT 'Sci-Fi',
    ADD COLUMN platform VARCHAR(100) NULL,
    ADD COLUMN stock_quantity INT NOT NULL DEFAULT 0;

UPDATE movies SET genre = 'Sci-Fi', platform = 'Blu-ray', stock_quantity = 10 WHERE title = 'Inception';
UPDATE movies SET genre = 'Sci-Fi', platform = 'DVD', stock_quantity = 8 WHERE title = 'The Matrix';
UPDATE movies SET genre = 'Sci-Fi', platform = 'Digital', stock_quantity = 6 WHERE title = 'Interstellar';
UPDATE movies SET genre = 'Action', platform = 'Blu-ray', stock_quantity = 7 WHERE title = 'The Dark Knight';
UPDATE movies SET genre = 'Crime', platform = 'DVD', stock_quantity = 5 WHERE title = 'Pulp Fiction';