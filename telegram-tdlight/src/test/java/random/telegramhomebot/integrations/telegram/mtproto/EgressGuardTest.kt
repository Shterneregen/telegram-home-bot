package random.telegramhomebot.integrations.telegram.mtproto

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EgressGuardTest {
    @Test fun `ordinary unprivileged test process cannot assert guarded startup`() {
        assertThrows(Exception::class.java) { EgressGuard.verify(MtprotoProperties()) }
    }
}
