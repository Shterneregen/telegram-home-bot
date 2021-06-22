package random.telegramhomebot.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "feature_switcher")
class FeatureSwitcher(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "org.hibernate.type.UUIDCharType")
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    var id: UUID? = null,
    var name: String,
    var enabled: Boolean
) {
    constructor(name: String, enabled: Boolean) : this(null, name, enabled)
}
