DROP TABLE IF EXISTS authors;

CREATE TABLE authors
(
    -- BIGSERIALは自動インクリメントのBIGINT型
    id          BIGSERIAL PRIMARY KEY,
    -- ファーストネーム (必須)
    first_name  VARCHAR(255) NOT NULL,
    -- ミドルネーム (任意)
    middle_name VARCHAR(255),
    -- ラストネーム (必須)
    last_name   VARCHAR(255) NOT NULL,
    -- 生年月日
    birth_date  DATE         NOT NULL,
    -- 生年月日が現在の日付よりも過去であることを保証するCHECK制約
    CHECK (birth_date < CURRENT_DATE)
);
