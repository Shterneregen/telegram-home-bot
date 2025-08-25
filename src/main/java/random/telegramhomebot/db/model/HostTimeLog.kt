package random.telegramhomebot.db.model

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp

@Entity
@Table(name = "host_time_log")
class HostTimeLog(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "host_time_log_seq")
    @SequenceGenerator(name = "host_time_log_seq", sequenceName = "host_time_log_seq", allocationSize = 1)
    @Column(updatable = false, nullable = false)
    var id: Long? = null,
    @CreationTimestamp @Column(updatable = false)
    var createdDate: Timestamp? = null,
    @JoinColumn(name = "host_id") @ManyToOne @JsonBackReference
    var host: Host,
    var state: HostState
) {
    constructor(host: Host, state: HostState) : this(null, null, host, state)
}
