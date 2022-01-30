package random.telegramhomebot.auth.db.entities

import javax.persistence.Column
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table

@Table(name = "USER")
class User(
    @Column(name = "USERNAME", nullable = false, unique = true)
    var username: String,
    @Column(name = "PASSWORD")
    var password: String,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var enabled: Boolean?
) {
    @Id
    @Column(name = "USER_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "users_roles",
        joinColumns = [JoinColumn(name = "user_id", referencedColumnName = "USER_ID")],
        inverseJoinColumns = [JoinColumn(name = "role_id", referencedColumnName = "id")]
    )
    var roles: List<Role>? = null
}
