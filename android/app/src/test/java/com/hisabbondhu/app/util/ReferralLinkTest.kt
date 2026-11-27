package com.hisabbondhu.app.util

import com.hisabbondhu.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferralLinkTest {
    @Test
    fun `play package is the published app id`() {
        assertEquals("com.hisabbondhu.myapp", Share.PLAY_PACKAGE)
        assertEquals("https://play.google.com/store/apps/details?id=com.hisabbondhu.myapp", Share.PLAY_URL)
        // Debug builds add ".debug"; the release id must stay exactly the Play Console app.
        assertTrue(BuildConfig.APPLICATION_ID.startsWith(Share.PLAY_PACKAGE))
    }

    @Test
    fun `referral code comes through the Play install referrer`() {
        assertEquals("K7M2QX", InstallReferrer.parse("ref=K7M2QX"))
        assertEquals("K7M2QX", InstallReferrer.parse("ref%3Dk7m2qx"))           // still URL-encoded
        assertEquals("K7M2QX", InstallReferrer.parse("utm_source=x&ref=K7M-2QX"))
        assertNull(InstallReferrer.parse("utm_source=google-play&utm_medium=organic"))
        assertNull(InstallReferrer.parse(null))
        assertNull(InstallReferrer.parse(""))
        assertNull(InstallReferrer.parse("preref=K7M2QX"))
    }

    @Test
    fun `typed codes are cleaned`() {
        assertEquals("K7M2QX", cleanReferralCode(" k7m-2qx "))
        assertEquals("ABCDEF", cleanReferralCode("abcdefgh"))
    }
}
