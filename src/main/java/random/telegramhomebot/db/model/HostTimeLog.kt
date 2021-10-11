package random.telegramhomebot.db.model

import com.fasterxml.jackson.annotation.JsonBackReference
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "host_time_logs")
class HostTimeLog(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "org.hibernate.type.UUIDCharType")
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    var id: UUID? = null,
    @CreationTimestamp @Column(updatable = false)
    var createdDate: Timestamp? = null,
    @JoinColumn(name = "host_id") @ManyToOne @JsonBackReference
    var host: Host,
    var state: HostState
) {
    constructor(host: Host, state: HostState) : this(null, null, host, state)
}
