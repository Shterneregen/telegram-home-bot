package random.telegramhomebot.services.csv

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import random.telegramhomebot.model.Host
import java.util.*
import java.util.stream.Collectors

@Component
class HostCsvConverter {
    fun convertHostToCsv(host: Host): HostCsv {
        return HostCsv(
            host.mac, host.deviceName, if (StringUtils.isNotBlank(host.notes)) Base64.getEncoder()
                .encodeToString(host.notes?.toByteArray()) else StringUtils.EMPTY
        )
    }

    fun convertHostListToCsvRows(hosts: List<Host?>): List<HostCsv> =
        hosts.stream().filter { it != null }.map { convertHostToCsv(it!!) }.collect(Collectors.toList())

    fun convertScvToHost(hostCsv: HostCsv): Host = Host(
        hostCsv.mac!!, hostCsv.deviceName.toString(),
        if (StringUtils.isNotBlank(hostCsv.notes)) String(
            Base64.getDecoder().decode(hostCsv.notes)
        ) else StringUtils.EMPTY
    )

    fun convertCsvRowsToHosts(hostCsvList: List<HostCsv>): List<Host> =
        hostCsvList.stream().map { hostCsv -> convertScvToHost(hostCsv) }.collect(Collectors.toList())
}