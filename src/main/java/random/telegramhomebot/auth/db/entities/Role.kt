package random.telegramhomebot.auth.db.entities

import jakarta.persistence.*

@Entity
@Table(name = "role")
class Role(
    var name: String,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "roles_privileges",
        joinColumns = [JoinColumn(name = "role_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "privilege_id", referencedColumnName = "id")]
    ) var privileges: List<Privilege>
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
    @SequenceGenerator(name = "role_seq", sequenceName = "role_seq", allocationSize = 1)
    var id: Long? = null

    @ManyToMany(mappedBy = "roles")
    var users: List<User>? = null
}
