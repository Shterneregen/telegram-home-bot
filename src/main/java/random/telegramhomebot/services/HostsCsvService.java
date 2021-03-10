package random.telegramhomebot.services;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static random.telegramhomebot.AppConstants.DATE_TIME_FORMATTER;

@RequiredArgsConstructor
@Slf4j
@Service
public class HostsCsvService {

	private static final String CSV_FILENAME_PATTERN = "hosts_%s.csv";

	private final HostService hostService;
	private final HostCsvConverter hostCsvConverter;

	public List<Host> parseHostsFromCsvFile(MultipartFile file) throws IOException {
		try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

			CsvToBean<HostCsv> csvToBeanHosts = new CsvToBeanBuilder<HostCsv>(reader)
					.withType(HostCsv.class)
					.withIgnoreLeadingWhiteSpace(true)
					.withIgnoreEmptyLine(true)
					.build();

			List<Host> convertedHosts = hostCsvConverter.convertCsvListToHosts(csvToBeanHosts.parse());
			List<Host> parsedHosts = convertedHosts.stream()
					.filter(hostFromCsv -> hostFromCsv.getMac() != null)
					.filter(hostFromCsv -> validateMac(hostFromCsv.getMac()))
					.map(hostFromCsv -> {
						String macFromCsv = hostFromCsv.getMac();
						Optional<Host> storedHostOp = hostService.getHostByMac(macFromCsv);
						if (storedHostOp.isPresent()) {
							Host host = storedHostOp.get();
							host.setDeviceName(hostFromCsv.getDeviceName());
							host.setNotes(hostFromCsv.getNotes());
							return host;
						} else {
							hostFromCsv.setState(HostState.FAILED);
							return hostFromCsv;
						}
					}).collect(Collectors.toList());
			log.debug("In CSV file were found [{}] hosts", parsedHosts.size());
			return parsedHosts;
		}
	}

	public void exportHostsToCsvFile(HttpServletResponse response) throws CsvFieldAssignmentException, IOException {
		exportBeansToCsvFile(response, hostCsvConverter.convertHostListToCsvs(hostService.getAllHosts()));
	}

	public void exportBeansToCsvFile(HttpServletResponse response, List beans)
			throws CsvFieldAssignmentException, IOException {
		String filename = String.format(CSV_FILENAME_PATTERN, LocalDateTime.now().format(DATE_TIME_FORMATTER));

		response.setContentType("text/csv");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

		StatefulBeanToCsv writer = new StatefulBeanToCsvBuilder<>(response.getWriter())
				.withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
				.withSeparator(CSVWriter.DEFAULT_SEPARATOR)
				.withOrderedResults(false)
				.build();

		writer.write(beans);
	}

	public boolean validateMac(String mac) {
		Pattern p = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
		Matcher m = p.matcher(mac);
		return m.find();
	}
}
