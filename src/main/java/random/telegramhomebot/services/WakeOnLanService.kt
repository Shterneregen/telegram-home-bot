package random.telegramhomebot.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import random.telegramhomebot.utils.logger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@Service
class WakeOnLanService {
    val log = logger()
    val macBytesSize = 6

    @Value("\${wakeOnLan.port}")
    private lateinit var port: Number

    @Value("\${wakeOnLan.broadcast.ip}")
    private lateinit var broadcastIp: String

    fun wakeOnLan(mac: String?, ip: String = broadcastIp) {
        if (mac == null) return
        try {
            val magicPacketBytes = getMagicPacketBytes(mac)
            val socket = DatagramSocket()
            socket.send(
                DatagramPacket(magicPacketBytes, magicPacketBytes.size, InetAddress.getByName(ip), port.toInt())
            )
            socket.close()
        } catch (e: Exception) {
            log.error("Failed to send Wake-on-LAN packet: {}", e.message)
            log.error(e.message, e)
        }
    }

    private fun getMagicPacketBytes(mac: String): ByteArray {
        val macBytes = getMacBytes(mac)
        val packetSize = macBytesSize + 16 * macBytes.size
        val magicPacketBytes = ByteArray(packetSize)
        for (i in 0 until macBytesSize) magicPacketBytes[i] = 0xff.toByte()
        for (item in macBytesSize.until(magicPacketBytes.size).step(macBytes.size))
            System.arraycopy(macBytes, 0, magicPacketBytes, item, macBytes.size)
        return magicPacketBytes
    }

    private fun getMacBytes(mac: String): ByteArray {
        val hex: List<String> = mac.split(":", "-")
        if (hex.size != macBytesSize) throw IllegalArgumentException("Invalid MAC address")

        val bytes = ByteArray(macBytesSize)
        for (i in 0 until macBytesSize) bytes[i] = hex[i].toInt(radix = 16).toByte()

        return bytes
    }
}
