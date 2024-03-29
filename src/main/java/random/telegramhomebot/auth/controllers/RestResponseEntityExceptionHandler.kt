package random.telegramhomebot.auth.controllers

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import random.telegramhomebot.auth.dto.GenericResponse
import random.telegramhomebot.auth.exceptinos.InvalidOldPasswordException
import random.telegramhomebot.services.messages.MessageService

@ControllerAdvice
class RestResponseEntityExceptionHandler(private val messageService: MessageService) :
    ResponseEntityExceptionHandler() {

    // 400
    override fun handleBindException(
        ex: BindException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error("400 Status Code", ex)
        val result = ex.bindingResult
        val bodyOfResponse = GenericResponse(result.allErrors, "Invalid: ${result.objectName}")
        return handleExceptionInternal(ex, bodyOfResponse, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error("400 Status Code", ex)
        val result = ex.bindingResult
        val bodyOfResponse = GenericResponse(result.allErrors, "Invalid: ${result.objectName}")
        return handleExceptionInternal(ex, bodyOfResponse, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(InvalidOldPasswordException::class)
    fun handleInvalidOldPassword(ex: RuntimeException, request: WebRequest): ResponseEntity<Any> {
        logger.error("400 Status Code", ex)
        val bodyOfResponse =
            GenericResponse(messageService.getMessage("message.invalidOldPassword"), "InvalidOldPassword")
        return handleExceptionInternal(ex, bodyOfResponse, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleInternal(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        logger.error("500 Status Code", ex)
        val bodyOfResponse = GenericResponse(messageService.getMessage("message.error"), "InternalError")
        return ResponseEntity(bodyOfResponse, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
