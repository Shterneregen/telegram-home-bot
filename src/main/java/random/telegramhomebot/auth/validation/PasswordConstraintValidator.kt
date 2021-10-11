package random.telegramhomebot.auth.validation

import com.google.common.base.Joiner
import org.passay.LengthRule
import org.passay.PasswordData
import org.passay.PasswordValidator
import org.passay.WhitespaceRule
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class PasswordConstraintValidator : ConstraintValidator<ValidPassword, String> {

    override fun initialize(constraintAnnotation: ValidPassword) {}
    override fun isValid(password: String, context: ConstraintValidatorContext): Boolean {
        val validator = PasswordValidator(
            listOf(
                LengthRule(4, 30),
                WhitespaceRule() /*,
                new UppercaseCharacterRule(1),
                new DigitCharacterRule(1),
                new SpecialCharacterRule(1),
                new NumericalSequenceRule(3, false),
                new AlphabeticalSequenceRule(3, false),
                new QwertySequenceRule(3, false)*/
            )
        )
        val result = validator.validate(PasswordData(password))
        if (result.isValid) {
            return true
        }
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(
            Joiner.on(",").join(validator.getMessages(result))
        ).addConstraintViolation()
        return false
    }
}
