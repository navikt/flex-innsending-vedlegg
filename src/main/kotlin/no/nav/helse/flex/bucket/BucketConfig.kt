package no.nav.helse.flex.no.nav.helse.flex.bucket

import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.threeten.bp.Duration

@Configuration
class BucketConfig {

    @Bean
    @Profile("default")
    fun storage(): Storage {
        val retrySettings = RetrySettings.newBuilder().setTotalTimeout(Duration.ofMillis(3000)).build()
        return StorageOptions.newBuilder().setRetrySettings(retrySettings).build().service
    }
}
