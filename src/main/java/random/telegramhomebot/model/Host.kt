package random.telegramhomebot.model

import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "hosts")
class Host(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "org.hibernate.type.UUIDCharType")
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    var id: UUID? = null,

    @field:JsonProperty("dst")
    var ip: String? = null,

    @Column(name = "host_interface")
    @field:JsonProperty("dev")
    var hostInterface: String? = null,

    @field:NotBlank(message = "{host.mac.should.be.not.empty}")
    @Column(unique = true, nullable = false)
    @field:JsonProperty("lladdr")
    var mac: String? = null,

    @field:JsonProperty("state")
    @field:JsonDeserialize(using = HostStateDeserializer::class)
    var state: HostState? = null,

    @Column(name = "device_name")
    var deviceName: String? = null,

    @field:JsonManagedReference
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "host")
    var timeLogs: List<HostTimeLog>? = null,

    var notes: String? = null
) {
    constructor(deviceName: String, mac: String, notes: String)
            : this(null, null, null, mac, null, deviceName, null, notes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val host = other as Host
        return mac == host.mac
    }

    override fun hashCode(): Int {
        return mac?.hashCode() ?: 0
    }
}
