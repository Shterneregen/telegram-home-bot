package random.telegramhomebot.services.csv;

import com.opencsv.exceptions.CsvFieldAssignmentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.services.HostService;
import random.telegramhomebot.utils.NetUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static random.telegramhomebot.AppConstants.DATE_TIME_FORMATTER;

@RequiredArgsConstructor
@Slf4j
@Service
public class HostsCsvService extends CsvService<HostCsv> {

    private static final String CSV_FILENAME_PATTERN = "hosts_%s.csv";

    private final HostService hostService;
    private final HostCsvConverter hostCsvConverter;

    public List<Host> parseHostsFromCsvFile(MultipartFile file) throws IOException {
        List<HostCsv> csvToBeanHosts = getBeansFromFile(file);
        List<Host> convertedHosts = hostCsvConverter.convertCsvRowsToHosts(csvToBeanHosts);
        List<Host> parsedHosts = prepareHostsAfterCsvParsing(convertedHosts);
        log.debug("In CSV file were found [{}] hosts", parsedHosts.size());
        return parsedHosts;
    }

    private List<Host> prepareHostsAfterCsvParsing(List<Host> convertedHosts) {
        return convertedHosts.stream()
                .filter(this::validateHostFromCsv)
                .map(this::prepareHostAfterCsvParsing)
                .collect(Collectors.toList());
    }

    private boolean validateHostFromCsv(Host host) {
        return host.getMac() != null
               && NetUtils.validateMac(host.getMac());
    }

    private Host prepareHostAfterCsvParsing(Host hostFromCsv) {
        String macFromCsv = hostFromCsv.getMac();
        Optional<Host> storedHostOp = hostService.getHostByMac(macFromCsv);
        if (storedHostOp.isPresent()) {
            Host host = storedHostOp.get();
            host.setDeviceName(hostFromCsv.getDeviceName());
            host.setNotes(hostFromCsv.getNotes());
            return host;
        }
        hostFromCsv.setState(HostState.FAILED);
        return hostFromCsv;
    }

    public void exportHostsToCsvFile(HttpServletResponse response) throws CsvFieldAssignmentException, IOException {
        exportBeansToCsvFile(response, hostCsvConverter.convertHostListToCsvRows(hostService.getAllHosts()),
                String.format(CSV_FILENAME_PATTERN, LocalDateTime.now().format(DATE_TIME_FORMATTER)));
    }
}
