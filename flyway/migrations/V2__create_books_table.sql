DROP TABLE IF EXISTS books;

-- books テーブルの作成
CREATE TABLE books (
    -- BIGSERIALは自動インクリメントのBIGINT型
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       price DECIMAL(10, 2) NOT NULL,
    -- 価格は小数点以下2桁まで許容(円以外のケースも考慮)
                       publication_status INT NOT NULL,
    -- 価格が0以上であることを保証するCHECK制約
                       CHECK (price >= 0),
    -- publication_statusが''0:未出版'または'1:出版済'のいずれかであることを保証するCHECK制約
                       CHECK (publication_status IN (0, 1))
);