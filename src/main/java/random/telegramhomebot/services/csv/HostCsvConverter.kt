package random.telegramhomebot.services.csv

import org.springframework.stereotype.Component
import random.telegramhomebot.model.Host
import java.util.*

@Component
class HostCsvConverter {
    fun convertHostToCsv(host: Host): HostCsv {
        val notes =
            if (host.notes?.isNotBlank() == true) Base64.getEncoder().encodeToString(host.notes?.toByteArray()) else ""
        return HostCsv(host.mac, host.deviceName, notes)
    }

    fun convertHostListToCsvRows(hosts: List<Host?>): List<HostCsv> =
        hosts.filterNotNull().map { convertHostToCsv(it) }

    fun convertScvToHost(hostCsv: HostCsv): Host {
        val notes = if (hostCsv.notes?.isNotBlank() == true) String(Base64.getDecoder().decode(hostCsv.notes)) else ""
        return Host(hostCsv.mac!!, hostCsv.deviceName.toString(), notes)
    }

    fun convertCsvRowsToHosts(hostCsvList: List<HostCsv>): List<Host> = hostCsvList.map { convertScvToHost(it) }
}