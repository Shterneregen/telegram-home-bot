package random.telegramhomebot.auth.controllers

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView
import random.telegramhomebot.auth.UserPrincipal
import random.telegramhomebot.auth.dto.GenericResponse
import random.telegramhomebot.auth.dto.PasswordDto
import random.telegramhomebot.auth.exceptinos.InvalidOldPasswordException
import random.telegramhomebot.auth.services.UserService
import random.telegramhomebot.services.messages.MessageService
import reactor.core.publisher.Mono
// import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@Controller
class ChangePasswordController(private val messageService: MessageService, private val userService: UserService) {

    @GetMapping("/updatePassword")
    fun updatePassword(
//        request: HttpServletRequest?,
        model: ModelMap,
        @RequestParam("messageKey", required = false) messageKey: String?
    ): ModelAndView {
        messageKey?.let { key: String? ->
            val message = messageService.getMessage(key!!)
            model.addAttribute("message", message)
        }
        return ModelAndView("updatePassword", model)
    }

    @ResponseBody
    @PostMapping("/user/updatePassword")
    fun changeUserPassword(passwordDto: @Valid PasswordDto): Mono<GenericResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal

//        val user = userService.getUserByID(userPrincipal.id) ?: throw RuntimeException("Cannot get user")
//        if (!userService.checkIfValidOldPassword(user, passwordDto.oldPassword)) {
//            throw InvalidOldPasswordException()
//        }
//        userService.changeUserPassword(user, passwordDto.newPassword)
//        return GenericResponse(messageService.getMessage("message.updatePasswordSuc"))

        return userService.getUserByID(userPrincipal.id)
            .switchIfEmpty(Mono.error(RuntimeException("Cannot get user")))
            .flatMap { user ->
                if (!userService.checkIfValidOldPassword(user, passwordDto.oldPassword)) {
                    return@flatMap Mono.error(InvalidOldPasswordException())
                }
                Mono.just(user)
            }
            .flatMap { user ->
                userService.changeUserPassword(user, passwordDto.newPassword)
                Mono.just(user)
            }
            .flatMap { Mono.just(GenericResponse(messageService.getMessage("message.updatePasswordSuc"))) }
    }
}
