CREATE UNIQUE INDEX unique_author_details_including_nulls
    ON authors (name, birth_date) NULLS NOT DISTINCT;