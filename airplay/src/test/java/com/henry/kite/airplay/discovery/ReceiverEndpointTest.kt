package com.henry.kite.airplay.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

/** The XGIMI Z7X case: `_airplay` :7001 is a stub, the real RTSP server is `_raop` :7002. */
class ReceiverEndpointTest {

    private val host = InetAddress.getByAddress(byteArrayOf(192.toByte(), 168.toByte(), 68.toByte(), 50))

    private fun receiver(port: Int, audioOnly: Boolean = false) = AirPlayReceiver(
        name = "我的Z7X 高亮版",
        address = host,
        port = port,
        deviceId = "09:65:3C:FC:77:B9",
        model = "AppleTV3,2",
        sourceVersion = "220.68",
        publicIdentifier = null,
        publicKeyHex = null,
        features = FeatureFlags(1L shl 7),
        isAudioOnly = audioOnly,
    )

    @Test
    fun `mirroring entry follows the raop port`() {
        val endpoint = receiver(7001).withRtspPort(7002)

        assertEquals(7002, endpoint.port)
        assertTrue(endpoint.hasAudioOnlyService)
    }

    @Test
    fun `shared port is left alone`() {
        val endpoint = receiver(7000).withRtspPort(7000)

        assertEquals(7000, endpoint.port)
        assertFalse(endpoint.hasAudioOnlyService)
    }

    @Test
    fun `audio only entry keeps its own port`() {
        val endpoint = receiver(7002, audioOnly = true).withRtspPort(7002)

        assertEquals(7002, endpoint.port)
        assertTrue(endpoint.isAudioOnly)
    }

    @Test
    fun `unknown raop port changes nothing`() {
        val endpoint = receiver(7001).withRtspPort(null)

        assertEquals(7001, endpoint.port)
        assertFalse(endpoint.hasAudioOnlyService)
    }
}
