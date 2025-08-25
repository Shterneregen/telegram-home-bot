package random.telegramhomebot.auth.db.entities

import jakarta.persistence.*

@Entity
@Table(name = "privilege")
class Privilege(var name: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "privilege_seq")
    @SequenceGenerator(name = "privilege_seq", sequenceName = "privilege_seq", allocationSize = 1)
    var id: Long? = null

    @ManyToMany(mappedBy = "privileges")
    var roles: List<Role>? = null
}
