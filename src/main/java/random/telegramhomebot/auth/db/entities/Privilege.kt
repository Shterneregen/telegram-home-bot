package random.telegramhomebot.auth.db.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany

@Entity
class Privilege(var name: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null

    @ManyToMany(mappedBy = "privileges")
    var roles: List<Role>? = null
}
