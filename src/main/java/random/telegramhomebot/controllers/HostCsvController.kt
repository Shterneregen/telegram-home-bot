package random.telegramhomebot.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.services.HostService;
import random.telegramhomebot.services.csv.HostsCsvService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.REDIRECT_HOSTS;
import static random.telegramhomebot.AppConstants.HostsCsv.FILE_REQ_PARAM;
import static random.telegramhomebot.AppConstants.HostsCsv.HOSTS_CSV_EXPORT_MAPPING;
import static random.telegramhomebot.AppConstants.HostsCsv.HOSTS_CSV_IMPORT_MAPPING;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(HOSTS_MAPPING)
public class HostCsvController {

    private final HostService hostService;
    private final HostsCsvService hostsCsvService;

    @GetMapping(HOSTS_CSV_EXPORT_MAPPING)
    public void exportHostsToCsvFile(HttpServletResponse response) throws Exception {
        hostsCsvService.exportHostsToCsvFile(response);
    }

    @PostMapping(HOSTS_CSV_IMPORT_MAPPING)
    public String importHostsFromCsvFile(@RequestParam(FILE_REQ_PARAM) MultipartFile file, Model model) {

        if (file.isEmpty()) {
            // TODO: impl error message
            log.error("File to import is empty!");
//			model.addAttribute("message", "Please select a CSV file to upload.");
        } else {
            try {
                List<Host> hostsToImport = hostsCsvService.parseHostsFromCsvFile(file);
                if (CollectionUtils.isNotEmpty(hostsToImport)) {
                    hostService.saveAllHosts(hostsToImport);
                }
            } catch (Exception e) {
                // TODO: impl error message
//				model.addAttribute("message", "An error occurred while processing the CSV file.");
                log.error(e.getMessage(), e);
            }
        }

        return REDIRECT_HOSTS;
    }
}
