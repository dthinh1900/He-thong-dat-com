-- Store menu image bytes in PostgreSQL, including images uploaded before Flyway adoption.
CREATE TABLE IF NOT EXISTS menu_image_content (
    menu_id BIGINT PRIMARY KEY REFERENCES menu_images(id) ON DELETE CASCADE,
    content_type VARCHAR(32) NOT NULL CHECK(content_type IN ('image/png','image/jpeg')),
    content BYTEA NOT NULL CHECK(octet_length(content) BETWEEN 1 AND 5242880)
);
