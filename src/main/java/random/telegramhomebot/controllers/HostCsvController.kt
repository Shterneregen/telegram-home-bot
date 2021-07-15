package random.telegramhomebot.controllers

import org.apache.commons.collections4.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING
import random.telegramhomebot.AppConstants.Hosts.REDIRECT_HOSTS
import random.telegramhomebot.AppConstants.HostsCsv.*
import random.telegramhomebot.services.HostService
import random.telegramhomebot.services.csv.HostsCsvService
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping(HOSTS_MAPPING)
class HostCsvController(
    private val hostService: HostService,
    private val hostsCsvService: HostsCsvService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @GetMapping(HOSTS_CSV_EXPORT_MAPPING)
    @Throws(Exception::class)
    fun exportHostsToCsvFile(response: HttpServletResponse?) {
        hostsCsvService.exportHostsToCsvFile(response)
    }

    @PostMapping(HOSTS_CSV_IMPORT_MAPPING)
    fun importHostsFromCsvFile(@RequestParam(FILE_REQ_PARAM) file: MultipartFile, model: Model?): String {
        if (file.isEmpty) {
            // TODO: impl error message
            log.error("File to import is empty!")
            //			model.addAttribute("message", "Please select a CSV file to upload.");
        } else {
            try {
                val hostsToImport = hostsCsvService.parseHostsFromCsvFile(file)
                if (CollectionUtils.isNotEmpty(hostsToImport)) {
                    hostService.saveAllHosts(hostsToImport)
                }
            } catch (e: Exception) {
                // TODO: impl error message
//				model.addAttribute("message", "An error occurred while processing the CSV file.");
                log.error(e.message, e)
            }
        }
        return REDIRECT_HOSTS
    }
}
