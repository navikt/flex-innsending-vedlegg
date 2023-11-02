package no.nav.helse.flex.bucket

import com.google.cloud.storage.Storage
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class LokalBucketConfig {

    @Bean
    @Profile("test")
    fun storage(): Storage {
        return LocalStorageHelper.getOptions().service
    }
}
