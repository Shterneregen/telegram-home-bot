package random.telegramhomebot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "hosts")
public class Host {
	@Column(name = "ip")
	@JsonProperty("dst")
	private String ip;
	@Column(name = "host_interface")
	@JsonProperty("dev")
	private String hostInterface;
	@Id
	@Column(name = "mac")
	@JsonProperty("lladdr")
	private String mac;
	@Column(name = "state")
	@JsonProperty("state")
	@JsonDeserialize(using = HostStateDeserializer.class)
	private HostState state;
	@Column(name = "device_name")
	private String deviceName;

	public Host() {
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
}
