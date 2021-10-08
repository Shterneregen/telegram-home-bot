package random.telegramhomebot.auth.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import random.telegramhomebot.auth.UserPrincipal;
import random.telegramhomebot.auth.db.entities.User;
import random.telegramhomebot.auth.dto.GenericResponse;
import random.telegramhomebot.auth.dto.PasswordDto;
import random.telegramhomebot.auth.exceptinos.InvalidOldPasswordException;
import random.telegramhomebot.auth.services.UserService;
import random.telegramhomebot.services.messages.MessageService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ChangePasswordController {

    private final MessageService messageService;
    private final UserService userService;

    @GetMapping("/updatePassword")
    public ModelAndView updatePassword(
            final HttpServletRequest request, final ModelMap model,
            @RequestParam("messageKey") final Optional<String> messageKey) {
        messageKey.ifPresent(key -> {
            String message = messageService.getMessage(key);
            model.addAttribute("message", message);
        });
        return new ModelAndView("updatePassword", model);
    }

    @ResponseBody
    @PostMapping("/user/updatePassword")
    public GenericResponse changeUserPassword(@Valid PasswordDto passwordDto) {
        UserPrincipal userPrincipal
                = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final User user = userService.getUserByID(userPrincipal.getId()).orElseThrow();
        if (!userService.checkIfValidOldPassword(user, passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        userService.changeUserPassword(user, passwordDto.getNewPassword());
        return new GenericResponse(messageService.getMessage("message.updatePasswordSuc"));
    }

}
