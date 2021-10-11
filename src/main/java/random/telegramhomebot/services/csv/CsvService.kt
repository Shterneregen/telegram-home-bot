package random.telegramhomebot.services.csv

import com.opencsv.CSVWriter
import com.opencsv.bean.CsvToBeanBuilder
import com.opencsv.bean.StatefulBeanToCsvBuilder
import com.opencsv.exceptions.CsvDataTypeMismatchException
import com.opencsv.exceptions.CsvFieldAssignmentException
import com.opencsv.exceptions.CsvRequiredFieldEmptyException
import org.springframework.http.HttpHeaders
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.reflect.ParameterizedType
import javax.servlet.http.HttpServletResponse

abstract class CsvService<T> {
    @Throws(IOException::class)
    fun getBeansFromFile(file: MultipartFile): List<T> {
        BufferedReader(InputStreamReader(file.inputStream)).use { reader ->
            val build = CsvToBeanBuilder<T>(reader)
                .withType(genericTypeClass)
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreEmptyLine(true)
                .build()
            return build.parse()
        }
    }

    @Throws(CsvFieldAssignmentException::class, IOException::class)
    fun exportBeansToCsvFile(response: HttpServletResponse, beans: List<T>, fileName: String?) {
        response.contentType = CSV_CONTENT_TYPE
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_VALUE_FORMAT, fileName))
        write(response.writer, beans)
    }

    @Throws(CsvDataTypeMismatchException::class, CsvRequiredFieldEmptyException::class)
    private fun write(printWriter: PrintWriter, beans: List<T>) {
        val writer = StatefulBeanToCsvBuilder<T>(printWriter)
            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
            .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
            .withOrderedResults(false)
            .build()
        writer.write(beans)
    }

    private val genericTypeClass: Class<T>
        get() = try {
            val className = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0].typeName
            Class.forName(className) as Class<T>
        } catch (e: Exception) {
            throw IllegalStateException("Class is not parametrized with generic type! Please use extends <> ")
        }

    companion object {
        private const val CSV_CONTENT_TYPE = "text/csv"
        private const val CONTENT_DISPOSITION_VALUE_FORMAT = "attachment; filename=\"%s\""
    }
}
