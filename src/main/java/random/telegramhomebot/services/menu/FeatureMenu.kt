package random.telegramhomebot.services.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.function.Supplier;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class FeatureMenu extends Menu {
    private Supplier<Boolean> featureMethod;

    public FeatureMenu(String message, Supplier<String> method, Supplier<Boolean> featureMethod) {
        super(message, method);
        this.featureMethod = featureMethod;
    }
}
