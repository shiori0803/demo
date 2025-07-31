DROP TABLE IF EXISTS authors;

CREATE TABLE authors (
    -- BIGSERIALは自動インクリメントのBIGINT型
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         birth_date DATE NOT NULL,
    -- 生年月日が現在の日付よりも過去であることを保証するCHECK制約
                         CHECK (birth_date < CURRENT_DATE)
);