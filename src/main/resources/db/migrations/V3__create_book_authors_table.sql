DROP TABLE IF EXISTS book_authors;

-- book_authors テーブルの作成 (多対多のリレーションシップ用)
CREATE TABLE book_authors
(
    book_id   BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    -- 複合主キー (book_idとauthor_idの組み合わせで一意)
    PRIMARY KEY (book_id, author_id),
    -- booksテーブルへの外部キー制約
    FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    -- authorsテーブルへの外部キー制約
    FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE CASCADE
);