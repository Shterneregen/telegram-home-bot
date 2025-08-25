package random.telegramhomebot.db.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "host")
class Host(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "host_seq")
    @SequenceGenerator(name = "host_seq", sequenceName = "host_seq", allocationSize = 1)
    @Column(updatable = false, nullable = false)
    var id: Long? = null,

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
    var wakeOnLanEnabled: Boolean = false
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
