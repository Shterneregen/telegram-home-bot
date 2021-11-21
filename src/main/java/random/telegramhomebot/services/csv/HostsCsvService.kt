package random.telegramhomebot.services.csv

import com.opencsv.exceptions.CsvFieldAssignmentException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import random.telegramhomebot.const.AppConstants.DATE_TIME_FORMATTER
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.db.model.HostState
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.utils.NetUtils
import random.telegramhomebot.utils.logger
import java.io.IOException
import java.time.LocalDateTime.now
import javax.servlet.http.HttpServletResponse

@Service
class HostsCsvService(
    private val hostService: HostService,
    private val hostCsvConverter: HostCsvConverter
) : CsvService<HostCsv>() {
    val log = logger()

    @Throws(IOException::class)
    fun parseHostsFromCsvFile(file: MultipartFile): List<Host> {
        val csvToBeanHosts = getBeansFromFile(file)
        val convertedHosts = hostCsvConverter.convertCsvRowsToHosts(csvToBeanHosts)
        val parsedHosts = prepareHostsAfterCsvParsing(convertedHosts)
        log.debug("In CSV file were found [{}] hosts", parsedHosts.size)
        return parsedHosts
    }

    @Throws(CsvFieldAssignmentException::class, IOException::class)
    fun exportHostsToCsvFile(response: HttpServletResponse) {
        exportBeansToCsvFile(
            response, hostCsvConverter.convertHostListToCsvRows(hostService.getAllHosts()),
            String.format(CSV_FILENAME_PATTERN, now().format(DATE_TIME_FORMATTER))
        )
    }

    private fun prepareHostsAfterCsvParsing(hosts: List<Host>): List<Host> {
        return hosts.filter { validateHostFromCsv(it) }.map { prepareHostAfterCsvParsing(it) }
    }

    private fun validateHostFromCsv(host: Host): Boolean {
        return host.mac != null && NetUtils.validateMac(host.mac)
    }

    private fun prepareHostAfterCsvParsing(hostFromCsv: Host): Host {
        hostService.getHostByMac(hostFromCsv.mac)?.let {
            it.deviceName = hostFromCsv.deviceName
            it.notes = hostFromCsv.notes
            return it
        }
        hostFromCsv.state = HostState.FAILED
        return hostFromCsv
    }

    companion object {
        private const val CSV_FILENAME_PATTERN = "hosts_%s.csv"
    }
}
