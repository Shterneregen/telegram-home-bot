package random.telegramhomebot.services.csv

import com.opencsv.exceptions.CsvFieldAssignmentException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import random.telegramhomebot.AppConstants.DATE_TIME_FORMATTER
import random.telegramhomebot.model.Host
import random.telegramhomebot.model.HostState
import random.telegramhomebot.services.HostService
import random.telegramhomebot.utils.NetUtils
import random.telegramhomebot.utils.logger
import java.io.IOException
import java.time.LocalDateTime.now
import java.util.stream.Collectors
import javax.servlet.http.HttpServletResponse

@Service
class HostsCsvService(
    private val hostService: HostService,
    private val hostCsvConverter: HostCsvConverter
) : CsvService<HostCsv>() {
    val log = logger()

    @Throws(IOException::class)
    fun parseHostsFromCsvFile(file: MultipartFile?): List<Host> {
        val csvToBeanHosts = getBeansFromFile(file)
        val convertedHosts = hostCsvConverter.convertCsvRowsToHosts(csvToBeanHosts)
        val parsedHosts = prepareHostsAfterCsvParsing(convertedHosts)
        log.debug("In CSV file were found [{}] hosts", parsedHosts.size)
        return parsedHosts
    }

    @Throws(CsvFieldAssignmentException::class, IOException::class)
    fun exportHostsToCsvFile(response: HttpServletResponse?) {
        exportBeansToCsvFile(
            response, hostCsvConverter.convertHostListToCsvRows(hostService.getAllHosts()),
            String.format(CSV_FILENAME_PATTERN, now().format(DATE_TIME_FORMATTER))
        )
    }

    private fun prepareHostsAfterCsvParsing(convertedHosts: List<Host>): List<Host> {
        return convertedHosts.stream()
            .filter { host -> validateHostFromCsv(host) }
            .map { hostFromCsv -> prepareHostAfterCsvParsing(hostFromCsv) }
            .collect(Collectors.toList())
    }

    private fun validateHostFromCsv(host: Host): Boolean {
        return host.mac != null && NetUtils.validateMac(host.mac)
    }

    private fun prepareHostAfterCsvParsing(hostFromCsv: Host): Host {
        val macFromCsv = hostFromCsv.mac
        val host = hostService.getHostByMac(macFromCsv)
        if (host != null) {
            host.deviceName = hostFromCsv.deviceName
            host.notes = hostFromCsv.notes
            return host
        }
        hostFromCsv.state = HostState.FAILED
        return hostFromCsv
    }

    companion object {
        private const val CSV_FILENAME_PATTERN = "hosts_%s.csv"
    }
}