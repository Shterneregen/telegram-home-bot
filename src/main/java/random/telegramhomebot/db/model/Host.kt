package random.telegramhomebot.db.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
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

    var ip: String? = null,

    @Column(name = "host_interface")
    var hostInterface: String? = null,

    @field:NotBlank(message = "{host.mac.should.be.not.empty}")
    @Column(unique = true, nullable = false)
    var mac: String? = null,

    var state: HostState? = null,

    @Column(name = "device_name")
    var deviceName: String? = null,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "host", cascade = [CascadeType.REMOVE])
    var timeLogs: List<HostTimeLog>? = null,

    var notes: String? = null,
    var wakeOnLanEnabled: Boolean? = false
) {
    constructor(mac: String, deviceName: String, notes: String) :
        this(null, null, null, mac, null, deviceName, null, notes)

    constructor(ip: String?, hostInterface: String?, mac: String?, state: HostState?, deviceName: String) :
        this(null, ip, hostInterface, mac, state, deviceName, null, null)

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
