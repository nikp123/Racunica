package com.github.nikp123.racunica.util

import android.app.Activity
import com.github.nikp123.racunica.R
import com.github.nikp123.racunica.data.Receipt
import com.github.nikp123.racunica.data.ReceiptStatus
import com.github.nikp123.racunica.data.ReceiptStore
import com.github.nikp123.racunica.data.ReceiptStoreInterface
import com.github.nikp123.racunica.data.Store
import com.github.nikp123.racunica.data.StoreStatus
import it.skrape.core.htmlDocument
import it.skrape.fetcher.BlockingFetcher
import it.skrape.fetcher.Request
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.pre
import it.skrape.selects.html5.span
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import javax.net.ssl.SSLHandshakeException
import kotlin.String
import kotlin.collections.map
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi



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


        val TAXCORE_VERSON_NUMBER = 3

        enum class MonetaryUnit {
            RSD,        // Serbia
            BAM;        // Bosnia

            companion object {
                fun fromStringOrThrow(str: String): MonetaryUnit {
                    return entries.first { it.toString() == str }
                }
            }
        }

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

        enum class TransactionType(val code: UByte) {
            SALE    (0u.toUByte()),
            REFUND  (1u.toUByte());

            companion object {
                fun fromNumberOrThrow(num: UByte): TransactionType {
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
        val transactionType: TransactionType

        val buyerID: String?

        // They exist but are useless to us
        //val encryptedInternalData: ByteArray
        //val signature: ByteArray

        // Custom values I've made for my self
        val unit:       MonetaryUnit
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
                "suf.purs.gov.rs" -> MonetaryUnit.RSD
                "suf.poreskaupravars.org" -> MonetaryUnit.BAM
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

                transactionType = TransactionType.fromNumberOrThrow(data[42].toUByte())

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
        internal fun fullScrape(
                scraper: BlockingFetcher<Request> = OkHttpFetcher
            ): Result<FullReceiptScrapeResult> {

            return try {
                // HttpFetcher is broken
                Result.success(skrape(scraper) {
                    request {
                        this.url = uri.toString()
                    }

                    response {
                        htmlDocument {
                            // I do apologize to the incomprehensibly complex functional
                            // monad-ahh shit spaghetti going on here
                            FullReceiptScrapeResult (
                                receiptText = (pre {
                                    withAttribute = "style" to "font-family:monospace"
                                    findAll {
                                        map {
                                            it.toString().lineSequence()
                                                .joinToString("\n") { line ->
                                                    val firstNonWhitespace = line.trimStart()
                                                    if (firstNonWhitespace.isEmpty())
                                                        line
                                                    else when (firstNonWhitespace[0]) {
                                                        '=', '<' -> firstNonWhitespace
                                                        else -> line
                                                    }
                                                }
                                        }
                                    }
                                }[0]
                                    .replace(
                                        "font-family:monospace",
                                        "font-family:monospace; width: fit-content; margin: 0 auto; text-align: center;"
                                    )),
                                store = Store(
                                    status = StoreStatus.ONLINE,
                                    code = this@SimpleReceipt.requestedBy + "-" + this@SimpleReceipt.signedBy,
                                    name = span { withId = "shopFullNameLabel"; findFirst { text } },
                                    usersName = null,
                                    country = this@SimpleReceipt.country,
                                    municipality = span { withId = "administrativeUnitLabel"; findFirst { text } },
                                    city = span { withId = "cityLabel"; findFirst { text } },
                                    address = span { withId = "addressLabel"; findFirst { text } },
                                    note = null,
                                ),
                            )
                        }
                    }
                })
            } catch(e: SSLHandshakeException) {
                Result.failure(FetchInvalidCertificate(e.message))
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

        companion object {
            fun getHumanAmount(amount: Long, unit: MonetaryUnit): Double {
                return when (unit) {
                    MonetaryUnit.RSD -> amount.toDouble() / 10000f
                    MonetaryUnit.BAM -> amount.toDouble() / 10000f
                }
            }
        }

        fun getHumanAmount(): Double {
            return getHumanAmount(this.totalAmount, this.unit)
        }
    }

    // Warning: You HAVE to call close() on this, otherwise the spinner will not disappear!
    // The idea was to migrate this to a separate class that will act as a factory for all
    // other bill types that are incompatible with each-other
    class ReceiptExtractor {
        enum class Country {
            RS,        // Serbia
            BA;        // Bosnia
        }

        class InvalidURIHost(message: String) : Exception(message)
        class NotAnURI(message: String?) : Exception(message)
        class EntryAlreadyExists() : Exception()

        private var uri: URI

        constructor(data: ByteArray) {
            try {
                uri = URI.create(String(data, StandardCharsets.UTF_8))

                // The idea is if we have multiple Tax systems that we are able to determine which
                // one we pick from here
                val country = when (uri.host) {
                    "suf.purs.gov.rs" -> Country.RS
                    "suf.poreskaupravars.org" -> Country.BA
                    else -> throw InvalidURIHost("No known tax authority matches")
                }
            } catch(e: InvalidURIHost) {
                throw e
            } catch(e: URISyntaxException) {
                // Here you should handle potentially other QR types, such as the Austrian one
                throw NotAnURI(e.message)
            }
        }

        // @return - Returns a pair of humanly readable receipt code and any potential error
        suspend fun processAndInsertReceipt(
            activity: Activity,
            receiptStoreInterface: ReceiptStoreInterface,
            existingReceiptID: Long? = null
        ): Pair<String?, Throwable?> {
            // Here we should branch out to provider specific implementations when they arrive
            val simple = SimpleReceipt(uri)

            // Access the database globally
            return withContext(Dispatchers.IO) {
                var (pair, error) = simple.fetchReceiptAndStore(activity, existingReceiptID)
                if(receiptStoreInterface.insertOrUpdate(pair) == -1L) {
                    error = EntryAlreadyExists()
                }

                // Update to non TaxCore specific way once you get to it
                val humanName = pair.store.code + '-' + pair.receipt.code

                Pair(humanName, error)
            }
        }

        companion object {
            // The idea with this is that we're able to turn machine errors to something
            // we can display to users
            // This is a sort-of error dictionary I guess
            fun receiptStatusToMessage(activity: Activity, code: String?, error: Throwable?, isUpdate: Boolean): String {
                val res = activity.resources

                return when (error) {
                    null -> if(isUpdate)
                        res.getString(R.string.receipt_updated, code)
                    else res.getString(R.string.receipt_added, code)
                    is EntryAlreadyExists -> res.getString(R.string.receipt_exists)
                    is SimpleReceipt.FetchInvalidCertificate -> res.getString(R.string.ssl_exception, error.message)
                    else -> res.getString(R.string.unknown_failure, error.message)
                }
            }
        }
    }
}

