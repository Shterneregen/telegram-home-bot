package random.telegramhomebot.auth.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import random.telegramhomebot.auth.dto.GenericResponse
import random.telegramhomebot.auth.exceptinos.InvalidOldPasswordException
import random.telegramhomebot.services.messages.MessageService

@ControllerAdvice
class RestResponseEntityExceptionHandler(private val messageService: MessageService) {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<Any> {
        val result = ex.bindingResult
        val bodyOfResponse = GenericResponse(result.allErrors, "Invalid: ${result.objectName}")
        return ResponseEntity(bodyOfResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidOldPasswordException::class)
    fun handleInvalidOldPassword(ex: InvalidOldPasswordException, request: WebRequest): ResponseEntity<Any> {
        val bodyOfResponse =
            GenericResponse(messageService.getMessage("message.invalidOldPassword"), "InvalidOldPassword")
        return ResponseEntity(bodyOfResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        val bodyOfResponse =
            GenericResponse(messageService.getMessage("message.error"), "Internal Server Error")
        return ResponseEntity(bodyOfResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}