-- Flyway migration: Insert seed data (movies and users)
INSERT INTO movies (title, description, price) VALUES
('Inception', 'A skilled thief who specializes in extraction, stealing valuable secrets from deep within the subconscious', 14.99),
('The Matrix', 'A computer programmer discovers that reality as he knows it is a simulation', 12.99),
('Interstellar', 'A team of explorers travel through a wormhole in space in an attempt to ensure humanity''s survival', 15.99),
('The Dark Knight', 'When the menace known as The Joker wreaks havoc on Gotham City, Batman must fight against him', 13.99),
('Pulp Fiction', 'The lives of two mob hitmen, a boxer, a gangster and his wife intertwine in four tales of violence', 11.99);

INSERT INTO users (email, name) VALUES
('alice@example.com', 'Alice'),
('bob@example.com', 'Bob'),
('charlie@example.com', 'Charlie');
