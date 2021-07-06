package random.telegramhomebot.services.csv;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import random.telegramhomebot.model.Host;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HostCsvConverter {

    public HostCsv convertHostToCsv(Host host) {
        return HostCsv.builder()
                .mac(host.getMac())
                .deviceName(host.getDeviceName())
                .notes(StringUtils.isNotBlank(host.getNotes())
                        ? Base64.getEncoder().encodeToString(host.getNotes().getBytes())
                        : StringUtils.EMPTY)
                .build();
    }

    public List<HostCsv> convertHostListToCsvRows(List<Host> hosts) {
        return hosts.stream().map(this::convertHostToCsv).collect(Collectors.toList());
    }

    public Host convertScvToHost(HostCsv hostCsv) {
        return Host.builder()
                .mac(hostCsv.getMac())
                .deviceName(hostCsv.getDeviceName())
                .notes(StringUtils.isNotBlank(hostCsv.getNotes())
                        ? new String(Base64.getDecoder().decode(hostCsv.getNotes()))
                        : StringUtils.EMPTY)
                .build();
    }

    public List<Host> convertCsvRowsToHosts(List<HostCsv> hostCsvList) {
        return hostCsvList.stream().map(this::convertScvToHost).collect(Collectors.toList());
    }
}
