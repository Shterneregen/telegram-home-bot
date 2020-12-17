package random.telegramhomebot.auth.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Entity
@Table(name = "AUTH_USER_GROUP")
public class AuthGroup {

	@Id
	@Column(name = "AUTH_USER_GROUP_ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@Column(name = "USERNAME")
	private String username;
	@Column(name = "AUTH_GROUP")
	private String authGroup;

	public AuthGroup(String username, String authGroup) {
		this.username = username;
		this.authGroup = authGroup;
	}
}
