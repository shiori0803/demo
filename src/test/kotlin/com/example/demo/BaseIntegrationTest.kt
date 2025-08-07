package com.example.demo

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

// 'open'を付けると他のテストクラスが継承できる
@SpringBootTest
@Testcontainers
open class BaseIntegrationTest {
    // テストで使う共通の定義はここに記載
    companion object {
        // Testcontainersのコンテナ定義
        @Container
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:16.2"))

        // 動的なプロパティ設定
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            val flyway =
                Flyway
                    .configure()
                    .dataSource(
                        postgres.jdbcUrl,
                        postgres.username,
                        postgres.password,
                    ).locations("classpath:db/migrations")
                    .load()
            flyway.migrate()
        }
    }
}
