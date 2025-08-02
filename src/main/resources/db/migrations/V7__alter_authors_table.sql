-- 既存のauthorsテーブルにUNIQUE制約を追加。
-- first_name, middle_name, last_name, birth_date の組み合わせが一意であることを保証。
ALTER TABLE authors
    ADD CONSTRAINT unique_author_details UNIQUE (first_name, middle_name, last_name, birth_date);