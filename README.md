# ER図
```mermaid
erDiagram
    authors {
        BIGSERIAL id PK
        VARCHAR first_name
        VARCHAR middle_name
        VARCHAR last_name
        DATE birth_date
    }

    books {
        BIGSERIAL id PK
        VARCHAR title
        INT price
        INT publication_status
    }

    book_authors {
        BIGINT book_id PK,FK
        BIGINT author_id PK,FK
    }

    authors ||--o{ book_authors : "has"
    books ||--o{ book_authors : "has"
```

# memo
- Intelijでmermaidの描画を確認できるプラグイン
  - [Jetbrains Marketplace - Mermaid](https://plugins.jetbrains.com/plugin/20146-mermaid)
