package random.telegramhomebot.services.menu

import java.util.function.Supplier

class FeatureMenu(
    message: String,
    method: Supplier<String>,
    var featureMethod: Supplier<Boolean>
) : Menu(message, method)