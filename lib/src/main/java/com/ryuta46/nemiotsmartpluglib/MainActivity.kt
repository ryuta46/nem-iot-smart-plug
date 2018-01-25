package com.ryuta46.nemiotsmartpluglib

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.google.gson.Gson
import com.ryuta46.nemkotlin.util.ConvertUtils
import com.ryuta46.nemkotlin.util.Logger
import java.security.SecureRandom


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {
    companion object {
        private val TAG = "NemIoTUnlockTest"
        private val ADDRESS = "NAEZYI6YPR4YIRN4EAWSP3GEYU6ATIXKTXSVBEU5"
        private val KEY_LENGTH = 15
        private val IFTTT_WEBHOOK_KEY = ""

        private val PRICE_MICRO_XEM = 0 * 1_000_000L
        private val VALID_PERIOD = 1 * 60 * 1000L
    }

    private val nemClient = NemClientController(
            mainHosts = listOf(
                    "www.ttechdev.com"
            ),
            logger = object : Logger {
                override fun log(level: Logger.Level, message: String) {
                    when (level) {
                        Logger.Level.Verbose -> Log.v(TAG, message)
                        Logger.Level.Debug -> Log.d(TAG, message)
                        Logger.Level.Info -> Log.i(TAG, message)
                        Logger.Level.Warn -> Log.w(TAG, message)
                        Logger.Level.Error -> Log.e(TAG, message)
                        else -> { /* Do nothing */
                        }
                    }
                }
            })

    private var prevKey: String = ""
    private var oneTimeKey: String = ""

    private lateinit var mIftttController: IftttController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mIftttController = IftttController(IFTTT_WEBHOOK_KEY)

        nemClient.subscribeBlocks {
            refreshCode()
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        refreshCode()
        startTransactionWatch()
    }

    private fun refreshCode() {
        runOnUiThread {
            val keyBytes = ByteArray(KEY_LENGTH)
            SecureRandom().nextBytes(keyBytes)
            prevKey = oneTimeKey
            oneTimeKey = ConvertUtils.toHexString(keyBytes)

            val invoiceData = InvoiceContainer(data = InvoiceData("COFFEE MAKER", ADDRESS, PRICE_MICRO_XEM, oneTimeKey))

            val qrCodeImage: ImageView = findViewById(R.id.imageQrCode)
            val size = Math.min(qrCodeImage.width, qrCodeImage.height)
            val bitmap = ZXingWrapper.createBitmap(Gson().toJson(invoiceData).toByteArray(), size, size)

            val drawable = BitmapDrawable(resources, bitmap).apply {
                setAntiAlias(false)
            }
            qrCodeImage.setImageDrawable(drawable)
        }

    }

    private fun startTransactionWatch() {
        nemClient.subscribeTransaction(ADDRESS) { _, message, amount ->
            if ( message == ConvertUtils.toHexString(oneTimeKey.toByteArray()) || message == ConvertUtils.toHexString(prevKey.toByteArray())) {
                if (amount >= PRICE_MICRO_XEM ) {
                    pass()
                }
            }
        }
    }

    private fun pass() {
        mIftttController.turnOn()
        Thread.sleep(VALID_PERIOD)
        mIftttController.turnOff()

    }
}