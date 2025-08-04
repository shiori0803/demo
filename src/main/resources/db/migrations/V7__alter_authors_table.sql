CREATE UNIQUE INDEX unique_author_details_including_nulls
    ON authors (first_name, middle_name, last_name, birth_date) NULLS NOT DISTINCT;