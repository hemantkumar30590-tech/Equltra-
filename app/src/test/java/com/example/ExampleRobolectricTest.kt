package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.EqPresetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify 31 band setup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Equltra", appName)

        // Verify 31-Band ISO 1/3-Octave configuration
        assertEquals(31, EqPresetEntity.BAND_COUNT)
        assertEquals(31, EqPresetEntity.FREQUENCY_LABELS.size)
        assertEquals(31, EqPresetEntity.FREQUENCY_HZ.size)
        assertEquals("20Hz", EqPresetEntity.FREQUENCY_LABELS.first())
        assertEquals("20kHz", EqPresetEntity.FREQUENCY_LABELS.last())

        // Verify all built-in and device presets interpolate cleanly to 31 bands
        EqPresetEntity.BUILT_IN_PRESETS.forEach { preset ->
            assertEquals(31, preset.getBandGains().size)
        }
        EqPresetEntity.DEVICE_PRESETS.forEach { device ->
            val gains = device.getBandGains()
            assertEquals(31, gains.size)
            assertTrue(gains.all { it in -15f..15f })
        }
    }
}

