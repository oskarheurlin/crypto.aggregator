package com.ekorn.crypto.aggregator

import com.ekorn.crypto.aggregator.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties::class)
class Application {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
