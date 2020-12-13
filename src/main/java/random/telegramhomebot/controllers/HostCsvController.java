package random.telegramhomebot.controllers;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.services.HostsCsvService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.REDIRECT_HOSTS;
import static random.telegramhomebot.AppConstants.HostsCsv.FILE_REQ_PARAM;
import static random.telegramhomebot.AppConstants.HostsCsv.HOSTS_CSV_EXPORT_MAPPING;
import static random.telegramhomebot.AppConstants.HostsCsv.HOSTS_CSV_IMPORT_MAPPING;

@Controller
@RequestMapping(HOSTS_MAPPING)
public class HostCsvController {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Resource
	private HostRepository hostRepository;
	@Resource
	private HostsCsvService hostsCsvService;

	@GetMapping(HOSTS_CSV_EXPORT_MAPPING)
	public void exportHostsToCsvFile(HttpServletResponse response) throws Exception {
		hostsCsvService.exportHostsToCsvFile(response, hostRepository.findAll());
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
					hostRepository.saveAll(hostsToImport);
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
