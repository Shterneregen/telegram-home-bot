package random.telegramhomebot.db.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import random.telegramhomebot.db.model.FeatureSwitcher
import random.telegramhomebot.services.FeatureSwitcherService.Features
import javax.annotation.Resource

@ExtendWith(SpringExtension::class)
@DataJpaTest
internal class FeatureSwitcherRepositoryTest {

    @Resource
    private lateinit var repository: FeatureSwitcherRepository

    @AfterEach
    fun tearDown() {
        repository.deleteAll().block()
    }

    @Test
    fun `should save feature`() {
        val feature = FeatureSwitcher(Features.NEW_HOSTS_NOTIFICATION.name, true)
        repository.save(feature).block()

        val featureFromDb = repository.findFeatureSwitcherByName(feature.name).block()
        assertEquals(feature.name, featureFromDb?.name)
    }

    @Test
    fun `should return all features`() {
        val features = listOf(
            FeatureSwitcher(Features.NEW_HOSTS_NOTIFICATION.name, true),
            FeatureSwitcher(Features.REACHABLE_HOSTS_NOTIFICATION.name, true),
            FeatureSwitcher(Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name, true)
        )
        repository.saveAll(features).subscribe()
        val featuresFromDb = repository.findAll().collectList().block()
        assertEquals(features.size, featuresFromDb.size)
    }

    @Test
    fun `should delete feature`() {

        val feature = FeatureSwitcher(Features.NEW_HOSTS_NOTIFICATION.name, true)
        repository.save(feature).block()

        repository.deleteById(feature.id!!).block()
        repository.findFeatureSwitcherByName(feature.name).block()

        assertNull(repository.findFeatureSwitcherByName(feature.name).block())
    }
}
