package random.telegramhomebot.services.csv

import com.opencsv.bean.CsvBindByPosition

class HostCsv(
    @CsvBindByPosition(position = 0) var mac: String? = null,
    @CsvBindByPosition(position = 1) var deviceName: String? = null,
    @CsvBindByPosition(position = 2) var notes: String? = null
)
