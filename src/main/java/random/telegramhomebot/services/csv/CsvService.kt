package random.telegramhomebot.services.csv;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.util.List;

public abstract class CsvService<T> {

    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String CONTENT_DISPOSITION_VALUE_FORMAT = "attachment; filename=\"%s\"";

    public List<T> getBeansFromFile(MultipartFile file) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<T> build = new CsvToBeanBuilder<T>(reader)
                    .withType(getGenericTypeClass())
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            return build.parse();
        }
    }

    public void exportBeansToCsvFile(HttpServletResponse response, List<T> beans, String fileName)
            throws CsvFieldAssignmentException, IOException {
        response.setContentType(CSV_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_VALUE_FORMAT, fileName));
        write(response.getWriter(), beans);
    }

    private void write(PrintWriter printWriter, List<T> beans)
            throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        StatefulBeanToCsv<T> writer = new StatefulBeanToCsvBuilder<T>(printWriter)
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(false)
                .build();
        writer.write(beans);
    }

    @SuppressWarnings("unchecked")
    private Class<T> getGenericTypeClass() {
        try {
            String className = ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0].getTypeName();
            return (Class<T>) Class.forName(className);
        } catch (Exception e) {
            throw new IllegalStateException("Class is not parametrized with generic type! Please use extends <> ");
        }
    }
}
