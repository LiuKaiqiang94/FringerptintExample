package com.liukq.fringerprint

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.widget.Toast

import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

/**
 * Created by Liukq on 2018/9/13
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var mKeyStore: KeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if (supportFingerprint()) {
            initKey()
            initCipher()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun initCipher() {
        try {
            val key = mKeyStore.getKey(DEFAULT_KEY_NAME, null) as SecretKey
            val cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            showFingerprint(cipher)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun showFingerprint(cipher: Cipher) {
        val fingerprintDialogFragment = FingerprintDialogFragment()
        fingerprintDialogFragment.setCipher(cipher)
        fingerprintDialogFragment.show(supportFragmentManager, "fingerprint")
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun initKey() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore")
            mKeyStore.load(null)
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val builder = KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun supportFingerprint(): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            Toast.makeText(this, "系统版本过低，不支持指纹功能", Toast.LENGTH_SHORT).show()
            return false
        } else {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            val fingerprintManager = getSystemService(FingerprintManager::class.java)
            if (!fingerprintManager!!.isHardwareDetected) {
                Toast.makeText(this, "手机不支持指纹功能", Toast.LENGTH_SHORT).show()
                return false
            } else if (!keyguardManager!!.isKeyguardSecure) {
                Toast.makeText(this, "手机手机未设置锁屏，先设置锁屏并添加一个指纹", Toast.LENGTH_SHORT).show()
                return false
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(this, "至少需要在系统中设置中添加一个指纹", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    fun onAuthenticated() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val DEFAULT_KEY_NAME = "default_key"
    }
}
