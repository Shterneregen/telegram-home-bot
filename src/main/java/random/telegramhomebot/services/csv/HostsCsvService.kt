package random.telegramhomebot.services.csv

import org.springframework.stereotype.Service
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.utils.logger

@Service
class HostsCsvService(
    private val hostService: HostService,
    private val hostCsvConverter: HostCsvConverter
) : CsvService<HostCsv>() {
    val log = logger()

//    @Throws(IOException::class)
//    fun parseHostsFromCsvFile(file: MultipartFile): List<Host> {
//        val csvToBeanHosts = getBeansFromFile(file)
//        val convertedHosts = hostCsvConverter.convertCsvRowsToHosts(csvToBeanHosts)
//        val parsedHosts = prepareHostsAfterCsvParsing(convertedHosts)
//        log.debug("In CSV file were found [{}] hosts", parsedHosts.size)
//        return parsedHosts
//    }

//    @Throws(CsvFieldAssignmentException::class, IOException::class)
//    fun exportHostsToCsvFile(response: HttpServletResponse) {
//        exportBeansToCsvFile(
//            response, hostCsvConverter.convertHostListToCsvRows(hostService.getAllHosts()),
//            String.format(CSV_FILENAME_PATTERN, now().format(DATE_TIME_FORMATTER))
//        )
//    }
//
//    private fun prepareHostsAfterCsvParsing(hosts: List<Host>): List<Host> {
//        return hosts.filter { validateHostFromCsv(it) }.map { prepareHostAfterCsvParsing(it) }
//    }
//
//    private fun validateHostFromCsv(host: Host): Boolean {
//        return host.mac != null && NetUtils.validateMac(host.mac)
//    }
//
//    private fun prepareHostAfterCsvParsing(hostFromCsv: Host): Host {
//        hostService.getHostByMac(hostFromCsv.mac)?.let {
//            it.deviceName = hostFromCsv.deviceName
//            it.notes = hostFromCsv.notes
//            return it
//        }
//        hostFromCsv.state = HostState.FAILED
//        return hostFromCsv
//    }

    companion object {
        private const val CSV_FILENAME_PATTERN = "hosts_%s.csv"
    }
}
