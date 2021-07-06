package random.telegramhomebot.services.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class HostCsv {
    @CsvBindByPosition(position = 0)
    private String mac;
    @CsvBindByPosition(position = 1)
    private String deviceName;
    @CsvBindByPosition(position = 2)
    private String notes;
}
