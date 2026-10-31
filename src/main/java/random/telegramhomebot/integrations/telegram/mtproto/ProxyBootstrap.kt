package random.telegramhomebot.integrations.telegram.mtproto

import it.tdlight.jni.TdApi
import java.util.concurrent.CompletableFuture

/** Authentication must await this gate. The host egress policy is still mandatory. */
internal class ProxyBootstrap(private val send: (TdApi.Function<*>) -> CompletableFuture<TdApi.Object>) {
    fun initialize(
        proxy: TdApi.Proxy,
        parameters: TdApi.SetTdlibParameters,
    ): CompletableFuture<TdApi.Object> {
        val disabled = send(TdApi.SetNetworkType(TdApi.NetworkTypeNone()))
        val added = send(TdApi.AddProxy(proxy, true, "THB"))
        // TDLib defers both requests until parameters arrive: never await them here.
        val initialized = send(parameters)
        return CompletableFuture.allOf(disabled, added, initialized).thenCompose {
            val result = added.join() as? TdApi.AddedProxy ?: error("Invalid proxy response")
            check(result.isEnabled) { "Proxy is disabled" }
            send(TdApi.GetProxies()).thenCompose { response ->
                val proxies = response as? TdApi.AddedProxies ?: error("Invalid proxy list")
                check(
                    proxies.proxies.any {
                        it.id == result.id && it.isEnabled &&
                            it.proxy.server == proxy.server && it.proxy.port == proxy.port &&
                            (it.proxy.type as? TdApi.ProxyTypeMtproto)?.secret == (proxy.type as TdApi.ProxyTypeMtproto).secret
                    },
                ) {
                    "Configured MTProto proxy is not enabled"
                }
                send(TdApi.SetNetworkType(TdApi.NetworkTypeOther()))
            }
        }
    }
}
