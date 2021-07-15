package random.telegramhomebot.services.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Menu {
    private String message;
    private Supplier<String> method;
}
