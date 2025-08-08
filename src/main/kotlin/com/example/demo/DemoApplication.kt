package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Bootアプリケーションのエントリーポイントとなるメインクラス。
 * `@SpringBootApplication`アノテーションにより、自動設定、コンポーネントスキャン、設定クラスの定義が有効になります。
 */
@SpringBootApplication
class DemoApplication

/**
 * アプリケーションを起動するメイン関数。
 */
fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
