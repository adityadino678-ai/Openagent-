package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OpenAgent", appName)
  }

  @Test
  fun `verify AES-256 local crypto encryption roundtrip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cryptoManager = CryptoManager(context)
    val secretKey = "sk-test-sample-openagent-key-12345"

    cryptoManager.saveEncryptedString("test_key", secretKey)
    val retrievedKey = cryptoManager.getEncryptedString("test_key")

    assertEquals(secretKey, retrievedKey)
  }
}
