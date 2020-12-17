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
@Table(name = "USER")
public class User {

	@Id
	@Column(name = "USER_ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@Column(name = "USERNAME", nullable = false, unique = true)
	private String username;
	@Column(name = "PASSWORD")
	private String password;

	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}
}
