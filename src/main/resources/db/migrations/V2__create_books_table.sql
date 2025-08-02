DROP TABLE IF EXISTS books;

-- books テーブルの作成
CREATE TABLE books
(
    -- BIGSERIALは自動インクリメントのBIGINT型
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    price              INT          NOT NULL, -- DECIMAL(10, 2) から INT に変更
    -- 価格は小数点以下を扱わない (円を想定)
    publication_status INT          NOT NULL,
    -- 価格が0以上であることを保証するCHECK制約
    CHECK (price >= 0),
    -- publication_statusが''0:未出版'または'1:出版済'のいずれかであることを保証するCHECK制約
    CHECK (publication_status IN (0, 1))
);