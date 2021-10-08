package random.telegramhomebot.auth.dto;

import lombok.Data;
import random.telegramhomebot.auth.validation.ValidPassword;

@Data
public class PasswordDto {
    private String oldPassword;
    private String token;
    @ValidPassword
    private String newPassword;
}
