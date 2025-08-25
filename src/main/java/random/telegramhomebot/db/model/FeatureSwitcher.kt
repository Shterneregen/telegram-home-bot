package random.telegramhomebot.db.model

import jakarta.persistence.*

@Entity
@Table(name = "feature_switcher")
class FeatureSwitcher(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feature_switcher_seq")
    @SequenceGenerator(name = "feature_switcher_seq", sequenceName = "feature_switcher_seq", allocationSize = 1)
    @Column(updatable = false, nullable = false)
    var id: Long? = null,
    var name: String,
    var enabled: Boolean
) {
    constructor(name: String, enabled: Boolean) : this(null, name, enabled)
}
