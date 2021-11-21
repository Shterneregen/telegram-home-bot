package random.telegramhomebot.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import random.telegramhomebot.utils.logger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * The magic packet is a frame that is most often sent as a broadcast and that contains anywhere within its payload
 * 6 bytes of all 255 (FF FF FF FF FF FF in hexadecimal), followed by sixteen repetitions of the target computer's
 * 48-bit MAC address, for a total of 102 bytes.
 * */
@Service
class WakeOnLanService {
    val log = logger()
    val numbersOfBytesInMac = 6
    val syncStreamSize = 6
    val macDuplicationCount = 16

    @Value("\${wakeOnLan.port}")
    private lateinit var port: Number

    @Value("\${wakeOnLan.broadcast.ip}")
    private lateinit var broadcastIp: String

    fun wakeOnLan(mac: String?, ip: String = broadcastIp) {
        if (mac == null) return
        try {
            DatagramSocket().run {
                send(getMagicPacket(mac, ip))
                close()
            }
        } catch (e: Exception) {
            log.error("Failed to send Wake-on-LAN packet: {}", e.message)
            log.error(e.message, e)
        }
    }

    private fun getMagicPacket(mac: String, ip: String) =
        getMagicPacketBytes(mac).let { DatagramPacket(it, it.size, InetAddress.getByName(ip), port.toInt()) }

    private fun getMagicPacketBytes(mac: String): ByteArray {
        val macBytes = getMacBytes(mac)
        return ByteArray(syncStreamSize + macDuplicationCount * macBytes.size).also { bytes ->
            // The Synchronization Stream is defined as 6 bytes of FFh
            for (i in 0 until syncStreamSize) bytes[i] = 0xff.toByte()
            // The Target MAC block contains 16 duplications of the IEEE address of the target, with no breaks or interruptions
            for (destPos in syncStreamSize.until(bytes.size).step(macBytes.size))
                System.arraycopy(macBytes, 0, bytes, destPos, macBytes.size)
        }
    }

    private fun getMacBytes(mac: String): ByteArray {
        val hex: List<String> = mac.split(":", "-")
        if (hex.size != numbersOfBytesInMac) throw IllegalArgumentException("Invalid MAC address")

        return ByteArray(hex.size).also { bytes ->
            for (i in hex.indices) bytes[i] = hex[i].toInt(radix = 16).toByte()
        }
    }
}
