package random.telegramhomebot.integrations.telegram.mtproto
import it.tdlight.jni.TdApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class ProxyBootstrapTest {
    @Test fun `different proxy readback never enables network`() {
        val proxy = TdApi.Proxy("example.invalid", 443, TdApi.ProxyTypeMtproto("expected"))
        val added =
            TdApi.AddedProxy().apply {
                id = 1
                isEnabled = true
                this.proxy = proxy
            }
        val mismatch =
            TdApi.AddedProxy().apply {
                id = 1
                isEnabled = true
                this.proxy = TdApi.Proxy("example.invalid", 443, TdApi.ProxyTypeMtproto("different"))
            }
        val requests = mutableListOf<TdApi.Function<*>>()
        val gate =
            ProxyBootstrap { query ->
                requests.add(query)
                CompletableFuture.completedFuture(
                    when (query) {
                        is TdApi.AddProxy -> added
                        is TdApi.GetProxies -> TdApi.AddedProxies(arrayOf(mismatch))
                        else -> TdApi.Ok()
                    },
                )
            }.initialize(proxy, TdApi.SetTdlibParameters())
        assertTrue(gate.isCompletedExceptionally)
        assertFalse(requests.any { it is TdApi.SetNetworkType && it.type is TdApi.NetworkTypeOther })
    }

    @Test fun `parameters precede waiting and authentication gate stays closed on failure`() {
        val requests = mutableListOf<TdApi.Function<*>>()
        val replies = mutableListOf<CompletableFuture<TdApi.Object>>()
        val gate =
            ProxyBootstrap { request ->
                requests.add(request)
                CompletableFuture<TdApi.Object>().also { replies.add(it) }
            }.initialize(TdApi.Proxy("example.invalid", 443, TdApi.ProxyTypeMtproto("secret")), TdApi.SetTdlibParameters())
        assertEquals(3, requests.size)
        assertTrue((requests[0] as TdApi.SetNetworkType).type is TdApi.NetworkTypeNone)
        assertTrue(requests[1] is TdApi.AddProxy)
        assertTrue(requests[2] is TdApi.SetTdlibParameters)
        assertFalse(gate.isDone)
        replies[0].complete(TdApi.Ok())
        replies[1].completeExceptionally(IllegalStateException("rejected"))
        replies[2].complete(TdApi.Ok())
        assertTrue(gate.isCompletedExceptionally)
        assertEquals(3, requests.size)
    }

    @Test fun `network enabled only after enabled proxy readback`() {
        val requests = mutableListOf<TdApi.Function<*>>()
        val proxy = TdApi.Proxy("example.invalid", 443, TdApi.ProxyTypeMtproto("secret"))
        val added =
            TdApi.AddedProxy().apply {
                id = 1
                isEnabled = true
                this.proxy = proxy
            }
        val gate =
            ProxyBootstrap { request ->
                requests.add(request)
                CompletableFuture.completedFuture(
                    when (request) {
                        is TdApi.AddProxy -> added
                        is TdApi.GetProxies -> TdApi.AddedProxies(arrayOf(added))
                        else -> TdApi.Ok()
                    },
                )
            }.initialize(proxy, TdApi.SetTdlibParameters())
        gate.join()
        assertEquals(5, requests.size)
        assertTrue(requests[3] is TdApi.GetProxies)
        assertTrue((requests[4] as TdApi.SetNetworkType).type is TdApi.NetworkTypeOther)
    }
}
