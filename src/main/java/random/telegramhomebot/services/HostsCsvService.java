package random.telegramhomebot.services;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static random.telegramhomebot.AppConstants.DATE_TIME_FORMATTER;

@Slf4j
@Service
public class HostsCsvService {

	private static final String CSV_FILENAME_PATTERN = "hosts_%s.csv";

	@Resource
	private HostRepository hostRepository;

	public List<Host> parseHostsFromCsvFile(MultipartFile file) throws IOException {
		try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

			CsvToBean<Host> csvToBean = new CsvToBeanBuilder<Host>(reader)
					.withType(Host.class)
					.withIgnoreLeadingWhiteSpace(true)
					.build();

			List<Host> hosts = csvToBean.parse();
			List<Host> parsedHosts = new ArrayList<>();
			hosts.stream()
					.filter(hostFromCsv -> hostFromCsv.getMac() != null)
					.filter(hostFromCsv -> validateMac(hostFromCsv.getMac()))
					.forEach(hostFromCsv -> {
						String macFromCsv = hostFromCsv.getMac();
						Host storedHost = hostRepository.findHostByMac(macFromCsv);
						if (storedHost != null) {
							storedHost.setDeviceName(hostFromCsv.getDeviceName());
							parsedHosts.add(storedHost);
						} else {
							parsedHosts.add(hostFromCsv);
						}
					});
			log.debug("In CSV file were found [{}] hosts", parsedHosts.size());
			return parsedHosts;
		}
	}

	public void exportHostsToCsvFile(HttpServletResponse response, List beans)
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
