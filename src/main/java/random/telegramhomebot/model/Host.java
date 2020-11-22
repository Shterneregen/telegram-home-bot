package random.telegramhomebot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.opencsv.bean.CsvBindByPosition;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "hosts")
public class Host {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Type(type = "org.hibernate.type.UUIDCharType")
	@Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
	private UUID id;
	@Column(name = "ip")
	@JsonProperty("dst")
	private String ip;
	@Column(name = "host_interface")
	@JsonProperty("dev")
	private String hostInterface;
	@CsvBindByPosition(position = 0)
	@NotBlank(message = "{host.mac.should.be.not.empty}")
	@Column(name = "mac", unique = true, nullable = false)
	@JsonProperty("lladdr")
	private String mac;
	@Column(name = "state")
	@JsonProperty("state")
	@JsonDeserialize(using = HostStateDeserializer.class)
	private HostState state;
	@CsvBindByPosition(position = 1)
	@Column(name = "device_name")
	private String deviceName;
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "host")
	private List<HostTimeLog> timeLogs;

	public Host() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getHostInterface() {
		return hostInterface;
	}

	public void setHostInterface(String hostInterface) {
		this.hostInterface = hostInterface;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public HostState getState() {
		return state;
	}

	public void setState(HostState state) {
		this.state = state;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public List<HostTimeLog> getTimeLogs() {
		return timeLogs;
	}

	public void setTimeLogs(List<HostTimeLog> timeLogs) {
		this.timeLogs = timeLogs;
	}

	@Override
	public String toString() {
		return "Host{" +
				"ip='" + ip + '\'' +
				", hostInterface='" + hostInterface + '\'' +
				", mac='" + mac + '\'' +
				", state=" + state +
				", deviceName='" + deviceName + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Host host = (Host) o;

		return Objects.equals(mac, host.mac);
	}

	@Override
	public int hashCode() {
		return mac != null ? mac.hashCode() : 0;
	}
}
