package random.telegramhomebot.services.menu

import java.util.function.Supplier

open class Menu(
    var buttonText: String,
    var method: Supplier<String>
)