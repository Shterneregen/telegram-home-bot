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
        repository.deleteAll()
    }

    @Test
    fun `should save feature`() {
        val feature = FeatureSwitcher(Features.NEW_HOSTS_NOTIFICATION.name, true)
        repository.save(feature)

        val featureFromDb = repository.findFeatureSwitcherByName(feature.name)
        assertEquals(feature.name, featureFromDb?.name)
    }

    @Test
    fun `should return all features`() {
        val features = listOf(
            FeatureSwitcher(Features.NEW_HOSTS_NOTIFICATION.name, true),
            FeatureSwitcher(Features.REACHABLE_HOSTS_NOTIFICATION.name, true),
            FeatureSwitcher(Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name, true)
        )
        repository.saveAll(features)
        val featuresFromDb = repository.findAll()
        assertEquals(features.size, featuresFromDb.size)
    }

    @Test
    fun `should delete feature`() {

        val feature = FeatureSwitcher(Features.NEW_HOSTS_NOTIFICATION.name, true)
        repository.save(feature)

        repository.deleteById(feature.id!!)
        repository.findFeatureSwitcherByName(feature.name)

        assertNull(repository.findFeatureSwitcherByName(feature.name))
    }
}
