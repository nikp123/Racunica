package com.github.nikp123.racunica.util

import android.app.Activity
import com.github.nikp123.racunica.data.Receipt
import com.github.nikp123.racunica.data.ReceiptStatus
import com.github.nikp123.racunica.data.ReceiptStore
import com.github.nikp123.racunica.data.Store
import com.github.nikp123.racunica.data.StoreStatus
import com.github.nikp123.racunica.util.ReceiptExtractor.TransactionType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import it.skrape.fetcher.BlockingFetcher
import it.skrape.fetcher.Request
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URI
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Locale
import javax.net.ssl.SSLHandshakeException
import kotlin.String
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.Result

fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { "%02X".format(it) }
}

class TaxCore {
    class SimpleReceipt {
        class BrokenURL(message: String) : Exception(message)
        class UnsupportedVersion(message: String) : Exception(message)
        class UnsupportedMonetaryValue(message: String) : Exception(message)
        class InvalidHash(message: String) : Exception(message)
        class DecodeError(message: String) : Exception(message)
        class UnsupportedCountry() : Exception()
        class FetchInvalidCertificate(message: String?) : Exception(message)
        class FetchGeneralFailure(message: String?) : Exception(message)
        class FetchParsingError(message: String?) : Exception(message)


        val TAXCORE_VERSON_NUMBER = 3

        enum class InvoiceType(val code: UByte) {
            NORMAL  (0u.toUByte()),
            PERFORMA(1u.toUByte()),
            COPY    (2u.toUByte()),
            TRAINING(3u.toUByte()),
            ADVANCE (4u.toUByte());

            companion object {
                fun fromNumberOrThrow(num: UByte): InvoiceType {
                    return entries.first { it.code == num }
                }
            }
        }

        val requestedBy: String
        val signedBy: String
        val totalTransactions: Int
        val totalTransactionsOfType: Int

        /**
         * For RSD, the value is represented by this number divided by 10000
         */
        val totalAmount: Long
        val timestamp: Long                    // 64bit UNIX epoch
        val invoiceType: InvoiceType
        val transactionType: ReceiptExtractor.TransactionType

        val buyerID: String?

        // They exist but are useless to us
        //val encryptedInternalData: ByteArray
        //val signature: ByteArray

        // Custom values I've made for my self
        val unit:       ReceiptExtractor.MonetaryUnit
        val uri:        URI
        val country:    ReceiptExtractor.Country

        /**
         * @param uri - Takes in the URL of the receipt obtained from the QR code
         */
        @Throws(UnsupportedMonetaryValue::class)
        @OptIn(ExperimentalEncodingApi::class)
        constructor(uri: URI) {
            this.uri = uri

            unit = when(uri.host) {
                "suf.purs.gov.rs" -> ReceiptExtractor.MonetaryUnit.RSD
                "suf.poreskaupravars.org" -> ReceiptExtractor.MonetaryUnit.BAM
                else -> {
                    throw UnsupportedMonetaryValue(
                        String.format("Unsupported tax provider (Got '%s')", uri.host)
                    )
                }
            }

            country = when(uri.host) {
                "suf.purs.gov.rs" -> ReceiptExtractor.Country.RS
                "suf.poreskaupravars.org" -> ReceiptExtractor.Country.BA
                else -> throw UnsupportedCountry()
            }

            // TODO: Check if this convention is valid for other countries as well
            if( uri.path != "/v/" ) {
                throw BrokenURL(
                    String.format("The URL path doesn't seem right? Have they changed the receipt format???? Expected: '/v/', got '%s'",
                        uri.path)
                )
            }

            // TODO: Same goes here
            val query: String? = URIUtils(uri).parseQuery()["vl"]
            if(query.isNullOrBlank()) {
                throw BrokenURL(
                    "The URL does not contain the vl parameter? The tax authority must've changed something!"
                )
            }

            val data = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
                .decode(query.toByteArray())

            if(data[0] != TAXCORE_VERSON_NUMBER.toByte()) {
                throw UnsupportedVersion(
                    String.format("Expected version %hhu (got %hhu)", TAXCORE_VERSON_NUMBER, data[0])
                )
            }

            val dataWithoutHash = data.sliceArray(0 until (data.size - 16))
            val hash = data.sliceArray(dataWithoutHash.size until data.size)
            val digest = MessageDigest.getInstance("MD5").digest(dataWithoutHash)

            if(!digest.contentEquals(hash)) {
                throw InvalidHash(
                    String.format("Expected: %s, got: %s", hash.toHexString(), digest.toHexString())
                )
            }

            try {
                requestedBy = String(data.sliceArray(1 until 9), Charset.forName("UTF-8"))
                signedBy = String(data.sliceArray(9 until 17), Charset.forName("UTF-8"))
                totalTransactions = (((data[20].toUInt() and 0xFFu) shl 24) or
                        ((data[19].toUInt() and 0xFFu) shl 16) or
                        ((data[18].toUInt() and 0xFFu) shl 8) or
                        (data[17].toUInt() and 0xFFu)).toInt()
                totalTransactionsOfType = (((data[24].toUInt() and 0xFFu) shl 24) or
                        ((data[23].toUInt() and 0xFFu) shl 16) or
                        ((data[22].toUInt() and 0xFFu) shl 8) or
                        (data[21].toUInt() and 0xFFu)).toInt()

                totalAmount = (((data[32].toULong() and 0xFFu) shl 56) or
                        ((data[31].toULong() and 0xFFu) shl 48) or
                        ((data[30].toULong() and 0xFFu) shl 40) or
                        ((data[29].toULong() and 0xFFu) shl 32) or
                        ((data[28].toULong() and 0xFFu) shl 24) or
                        ((data[27].toULong() and 0xFFu) shl 16) or
                        ((data[26].toULong() and 0xFFu) shl 8) or
                        (data[25].toULong() and 0xFFu)).toLong()

                // Spec issue: The QR code stores timestamps in ULongs
                // but the correct UNIX timestamp should be Long
                timestamp = (((data[33].toULong() and 0xFFu) shl 56) or
                        ((data[34].toULong() and 0xFFu) shl 48) or
                        ((data[35].toULong() and 0xFFu) shl 40) or
                        ((data[36].toULong() and 0xFFu) shl 32) or
                        ((data[37].toULong() and 0xFFu) shl 24) or
                        ((data[38].toULong() and 0xFFu) shl 16) or
                        ((data[39].toULong() and 0xFFu) shl 8) or
                        (data[40].toULong() and 0xFFu)).toLong()

                invoiceType = InvoiceType.fromNumberOrThrow(data[41].toUByte())

                transactionType = TransactionType.fromTaxCoreNumberOrThrow(data[42].toUByte())

                buyerID = when(data[43].toInt()) {
                    0 ->  null
                    else -> data.sliceArray(44 until (44 + (data[43].toInt())))
                        .toMutableList().add(0).toString()
                }

            } catch(e: Exception) {
                //Log.e("SimpleBillCtor", e.toString())
                // dump the entire bill for debugging purposes
                //Log.e("SimpleBillCtor", data.joinToString(" ") { "%02X".format(it) })
                throw DecodeError(e.message.toString())
            }
        }


        // @return - Returns the HTML of the receipt itself
        data class FullReceiptScrapeResult(
            val receiptText: String,
            val store: Store,
        )

        /**
         * There probably should be a prettier way to read a JSON response than this,
         * but this is my very short term research on how to do this using moshi's
         * README on GitHub.
         */
        class TaxCoreReceiptJsonInvoiceRequestPayment (
            val paymentType: Int,
            val amount: Float
        )

        class TaxCoreReceiptJsonInvoiceRequest (
            val posTime: String?,
            val taxId: String,
            val businessName: String,
            val locationName: String,
            val address: String,
            val city: String,
            val administrativeUnit: String,
            val buyer: String?,
            val buyerCostCenter: String?,
            val cashier: String,
            val requestedBy: String,
            val invoiceType: Int,
            val transactionType: Int,
            val payments: List<TaxCoreReceiptJsonInvoiceRequestPayment>
        )

        class TaxCoreReceiptJsonInvoiceResult (
            val totalAmount: Float,
            val transactionTypeCounter: Int,
            val totalCounter: Int,
            val invoiceCounterExtension: String,
            val invoiceNumber: String,
            val signedBy: String,
            val sdcTime: String
        )

        class TaxCoreReceiptJson (
            val invoiceRequest: TaxCoreReceiptJsonInvoiceRequest,
            val invoiceResult: TaxCoreReceiptJsonInvoiceResult,
            val journal: String,
            val isValid: Boolean
        )

        @OptIn(ExperimentalStdlibApi::class)
        internal fun fullScrapeVersion2(): Result<FullReceiptScrapeResult> {
            val headers = Headers.Builder()
                .add("Accept", "application/json")
                .build()

            val request = okhttp3.Request.Builder()
                .url(uri.toString())
                .headers(headers)
                .build()

            val result = try {
                OkHttpClient().newCall(request).execute()
            } catch(e: Exception) {
                return Result.failure(FetchGeneralFailure(e.message))
            }.body.string()

            val moshi: Moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val jsonAdapter: JsonAdapter<TaxCoreReceiptJson> = moshi.adapter<TaxCoreReceiptJson>()

            val receipt: TaxCoreReceiptJson = (jsonAdapter.fromJson(result.toString()))
                ?: return Result.failure(FetchParsingError("Unable to parse JSON"))

            return Result.success(FullReceiptScrapeResult (
                // The receipt text needs UNIX endlines because Android's WebView expects that
                receiptText = receipt.journal.replace("\r\n", "\n"),
                store = Store(
                    status = StoreStatus.ONLINE,
                    code = receipt.invoiceRequest.requestedBy +
                            "-" + receipt.invoiceResult.signedBy,
                    name = receipt.invoiceRequest.locationName +
                            " (" + receipt.invoiceRequest.businessName + ")",
                    usersName = receipt.invoiceRequest.buyer,
                    country = this@SimpleReceipt.country,
                    municipality = receipt.invoiceRequest.administrativeUnit,
                    city = receipt.invoiceRequest.city,
                    address = receipt.invoiceRequest.address,
                    note = null,
                )
            ))

        }

        internal fun fullScrape(
                scraper: BlockingFetcher<Request> = OkHttpFetcher
            ): Result<FullReceiptScrapeResult> {

            return try {
                // HttpFetcher is broken
                when(country) {
                    ReceiptExtractor.Country.RS -> fullScrapeVersion2()
                    ReceiptExtractor.Country.BA -> fullScrapeVersion2()
                    else -> throw UnsupportedCountry()
                }
            } catch(e: SSLHandshakeException) {
                Result.failure(FetchInvalidCertificate(e.message))
            } catch(e: FetchParsingError) {
                Result.failure(FetchParsingError(e.message))
            } catch(e: Exception) {
                Result.failure(FetchGeneralFailure(e.message))
            }
        }

        fun fetchReceiptAndStore(activity: Activity, assignExistingBillId: Long?): Pair<ReceiptStore, Throwable?> {
            val storeCode = this.requestedBy + "-" + this.signedBy

            val placeholder = FullReceiptScrapeResult(
                receiptText = "",
                store = Store (
                    status = StoreStatus.OFFLINE,
                    code = storeCode,
                    name = storeCode,
                    usersName = null,
                    country = this@SimpleReceipt.country,
                    municipality = null,
                    city         = null,
                    address      = null,
                    note         = null,
                )
            )

            var exception: Throwable? = null

            val result = when (activity.baseContext.hasInternetConnection()) {
                true -> {
                    val scrape = this.fullScrape()
                    if(scrape.isFailure) {
                        exception = scrape.exceptionOrNull()
                        placeholder
                    } else scrape.getOrThrow()
                }
                false -> { placeholder }
            }

            return Pair(ReceiptStore(
                receipt = Receipt(
                    id = assignExistingBillId ?: 0,
                    amount = this.totalAmount,
                    unit = this.unit,
                    time = this.timestamp,
                    storeID = 0, // This MUST be set later otherwise we got nasty bugs
                    code  = String.format(Locale.getDefault(), "%d", this.totalTransactions),
                    purchaserCode = this.buyerID,
                    status = when (result.receiptText.isEmpty()) {
                        true ->  ReceiptStatus.OFFLINE
                        false -> ReceiptStatus.ONLINE
                    },
                    type = this.transactionType,
                    text = when (result.receiptText.isEmpty()) {
                        true -> ""
                        false -> result.receiptText
                    },
                    country = this.country,
                    url = uri.toString(),
                    note = null,
                ),
                store = result.store
            ), exception)
        }
    }
}

