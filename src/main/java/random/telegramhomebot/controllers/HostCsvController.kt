package random.telegramhomebot.controllers

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING
import random.telegramhomebot.AppConstants.HostsCsv.*
import random.telegramhomebot.services.HostService
import random.telegramhomebot.services.csv.HostsCsvService
import random.telegramhomebot.utils.logger
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping(HOSTS_MAPPING)
class HostCsvController(
    private val hostService: HostService,
    private val hostsCsvService: HostsCsvService
) {
    val log = logger()

    @GetMapping(HOSTS_CSV_EXPORT_MAPPING)
    @Throws(Exception::class)
    fun exportHostsToCsvFile(response: HttpServletResponse?) {
        hostsCsvService.exportHostsToCsvFile(response)
    }

    @ResponseBody
    @PostMapping(HOSTS_CSV_IMPORT_MAPPING)
    fun importHostsFromCsvFile(
        @RequestParam(FILE_REQ_PARAM) file: MultipartFile
    ): ResponseEntity<String> {
        if (file.isEmpty) {
            log.error("File to import is empty!")
            return ResponseEntity.badRequest().body("Please select a CSV file to upload.")
        }
        return try {
            val hostsToImport = hostsCsvService.parseHostsFromCsvFile(file)
            if (hostsToImport.isNotEmpty()) {
                hostService.saveAllHosts(hostsToImport)
            }
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            val err = "An error occurred while processing the CSV file."
            log.error(e.message, e)
            ResponseEntity.badRequest().body(err)
        }
    }
}
