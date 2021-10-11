package random.telegramhomebot.auth.validation

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [PasswordConstraintValidator::class])
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidPassword(
    val message: String = "Invalid Password",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
