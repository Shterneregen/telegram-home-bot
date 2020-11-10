package random.telegramhomebot.controllers;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hosts")
public class HostController {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	private static final String HOSTS = "hosts";
	private static final String HOST = "host";
	private static final String REDIRECT_HOSTS = "redirect:/" + HOSTS;
	private static final String ADD_EDIT_HOST = "add-edit-host";

	@Resource
	private HostRepository hostRepository;

	@RequestMapping
	public String getAllCommands(Model model) {
		model.addAttribute(HOSTS, hostRepository.findAll().stream()
				.peek(host -> {
					if (host.getIp() == null) {
						host.setIp("");
					}
				})
				.sorted(Comparator.comparing(Host::getIp)).collect(Collectors.toList()));
		return HOSTS;
	}

	@RequestMapping(path = {"/edit", "/edit/{id}"})
	public String editHostById(Model model, @PathVariable("id") Optional<UUID> id) {
		model.addAttribute(HOST, Optional.ofNullable(id)
				.filter(Optional::isPresent)
				.flatMap(macOp -> hostRepository.findById(id.get()))
				.orElseGet(Host::new));
		return ADD_EDIT_HOST;
	}

	@RequestMapping(path = "/delete/{id}")
	public String deleteHostById(@PathVariable("id") UUID id) {
		hostRepository.deleteById(id);
		return REDIRECT_HOSTS;
	}

	@PostMapping(path = "/createHost")
	public String createOrUpdateHost(@Valid Host host, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ADD_EDIT_HOST;
		}
		Host storedHost = hostRepository.findHostByMac(host.getMac());
		boolean saveNewHostWithExistingMac = storedHost != null && host.getId() == null;
		boolean editStoredHostMacToExisting = storedHost != null && !storedHost.getId().equals(host.getId());
		if (saveNewHostWithExistingMac || editStoredHostMacToExisting) {
			bindingResult.rejectValue("mac", "host.mac.not.unique");
			return ADD_EDIT_HOST;
		}
		hostRepository.save(host);
		return REDIRECT_HOSTS;
	}

	@GetMapping("/export")
	public void exportCSV(HttpServletResponse response) throws Exception {

		String filename = String.format("hosts_%s.csv",
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy-ss")));

		response.setContentType("text/csv");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

		StatefulBeanToCsv<Host> writer = new StatefulBeanToCsvBuilder<Host>(response.getWriter())
				.withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
				.withSeparator(CSVWriter.DEFAULT_SEPARATOR)
				.withOrderedResults(false)
				.build();

		writer.write(hostRepository.findAll());
	}

	@PostMapping("/import")
	public String uploadCSVFile(@RequestParam("file") MultipartFile file, Model model) {

		if (file.isEmpty()) {
			// TODO: impl error message
//			model.addAttribute("message", "Please select a CSV file to upload.");
		} else {
			try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

				CsvToBean<Host> csvToBean = new CsvToBeanBuilder(reader)
						.withType(Host.class)
						.withIgnoreLeadingWhiteSpace(true)
						.build();

				List<Host> hosts = csvToBean.parse();
				List<Host> hostsToSave = new ArrayList<>();
				hosts.stream()
						.filter(hostFromCsv -> hostFromCsv.getMac() != null)
						.forEach(hostFromCsv -> {
							Host storedHost = hostRepository.findHostByMac(hostFromCsv.getMac());
							if (storedHost != null) {
								storedHost.setDeviceName(hostFromCsv.getDeviceName());
								hostsToSave.add(storedHost);
							} else {
								hostsToSave.add(hostFromCsv);
							}
						});
				if (hostsToSave.size() > 0) {
					hostRepository.saveAll(hostsToSave);
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
