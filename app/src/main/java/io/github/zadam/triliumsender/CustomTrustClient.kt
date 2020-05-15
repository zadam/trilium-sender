package io.github.zadam.triliumsender

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class CustomTrustClient {
    companion object {
        // PEM files for root certificates of Comodo and Entrust. These two CAs are sufficient to view
        // https://publicobject.com (Comodo) and https://squareup.com (Entrust). But they aren't
        // sufficient to connect to most HTTPS sites including https://godaddy.com and https://visa.com.
        // Typically developers will need to get a PEM file from their organization's TLS administrator.
        var certificateFactory: CertificateFactory? = null
        var letsencryptPem = """
            -----BEGIN CERTIFICATE-----
            MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/
            MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT
            DkRTVCBSb290IENBIFgzMB4XDTE2MDMxNzE2NDA0NloXDTIxMDMxNzE2NDA0Nlow
            SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxIzAhBgNVBAMT
            GkxldCdzIEVuY3J5cHQgQXV0aG9yaXR5IFgzMIIBIjANBgkqhkiG9w0BAQEFAAOC
            AQ8AMIIBCgKCAQEAnNMM8FrlLke3cl03g7NoYzDq1zUmGSXhvb418XCSL7e4S0EF
            q6meNQhY7LEqxGiHC6PjdeTm86dicbp5gWAf15Gan/PQeGdxyGkOlZHP/uaZ6WA8
            SMx+yk13EiSdRxta67nsHjcAHJyse6cF6s5K671B5TaYucv9bTyWaN8jKkKQDIZ0
            Z8h/pZq4UmEUEz9l6YKHy9v6Dlb2honzhT+Xhq+w3Brvaw2VFn3EK6BlspkENnWA
            a6xK8xuQSXgvopZPKiAlKQTGdMDQMc2PMTiVFrqoM7hD8bEfwzB/onkxEz0tNvjj
            /PIzark5McWvxI0NHWQWM6r6hCm21AvA2H3DkwIDAQABo4IBfTCCAXkwEgYDVR0T
            AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwfwYIKwYBBQUHAQEEczBxMDIG
            CCsGAQUFBzABhiZodHRwOi8vaXNyZy50cnVzdGlkLm9jc3AuaWRlbnRydXN0LmNv
            bTA7BggrBgEFBQcwAoYvaHR0cDovL2FwcHMuaWRlbnRydXN0LmNvbS9yb290cy9k
            c3Ryb290Y2F4My5wN2MwHwYDVR0jBBgwFoAUxKexpHsscfrb4UuQdf/EFWCFiRAw
            VAYDVR0gBE0wSzAIBgZngQwBAgEwPwYLKwYBBAGC3xMBAQEwMDAuBggrBgEFBQcC
            ARYiaHR0cDovL2Nwcy5yb290LXgxLmxldHNlbmNyeXB0Lm9yZzA8BgNVHR8ENTAz
            MDGgL6AthitodHRwOi8vY3JsLmlkZW50cnVzdC5jb20vRFNUUk9PVENBWDNDUkwu
            Y3JsMB0GA1UdDgQWBBSoSmpjBH3duubRObemRWXv86jsoTANBgkqhkiG9w0BAQsF
            AAOCAQEA3TPXEfNjWDjdGBX7CVW+dla5cEilaUcne8IkCJLxWh9KEik3JHRRHGJo
            uM2VcGfl96S8TihRzZvoroed6ti6WqEBmtzw3Wodatg+VyOeph4EYpr/1wXKtx8/
            wApIvJSwtmVi4MFU5aMqrSDE6ea73Mj2tcMyo5jMd6jmeWUHK8so/joWUoHOUgwu
            X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG
            PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6
            KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==
            -----END CERTIFICATE-----
            """.trimIndent()

        @Throws(CertificateException::class)
        private fun getCertFromPem(pem: String): X509Certificate {
            val buf = pem.toByteArray()
            val `is`: InputStream = ByteArrayInputStream(buf)
            return certificateFactory!!.generateCertificate(`is`) as X509Certificate
        }

        private var client: OkHttpClient? = null

        @Throws(CertificateException::class)
        fun initializeClientWithDefaultTrust(): OkHttpClient? {
            val certificates = HandshakeCertificates.Builder()
                    .addPlatformTrustedCertificates()
                    .addTrustedCertificate(getCertFromPem(letsencryptPem))
                    .build()
            client = OkHttpClient.Builder()
                    .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager())
                    .build()
            return client
        }

        @Throws(CertificateException::class)
        fun initializeClientWithCustomCert(pem: String): OkHttpClient? {
            val certificates = HandshakeCertificates.Builder()
                    .addTrustedCertificate(getCertFromPem(pem))
                    .build()
            client = OkHttpClient.Builder()
                    .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager())
                    .build()
            return client
        }

        @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
        fun initializeClientAndTrustAll(): OkHttpClient {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        @Throws(CertificateException::class)
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                        }

                        @Throws(CertificateException::class)
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return arrayOf()
                        }
                    }
            )

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { hostname, session -> true }
            return builder.build()
        }

        @Throws(CertificateException::class)
        fun getClient(): OkHttpClient? {
            if (client == null) {
                initializeClientWithDefaultTrust()
            }
            return client
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            CustomTrustClient().run()
        }

        init {
            try {
                certificateFactory = CertificateFactory.getInstance("X.509")
            } catch (e: CertificateException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(Exception::class)
    fun run() {
        val request = Request.Builder()
                .url("https://publicobject.com/helloworld.txt")
                .build()
        client!!.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val responseHeaders = response.headers()
                for (i in 0 until responseHeaders.size()) {
                    println(responseHeaders.name(i) + ": " + responseHeaders.value(i))
                }
                throw IOException("Unexpected code $response")
            }
            println(response.body()!!.string())
        }
    }
}