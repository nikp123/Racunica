package com.github.nikp123.racunica.util

import android.app.Activity
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import com.github.nikp123.racunica.R
import com.github.nikp123.racunica.data.ReceiptStoreInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// Warning: You HAVE to call close() on this, otherwise the spinner will not disappear!
// The idea was to migrate this to a separate class that will act as a factory for all
// other bill types that are incompatible with each-other
class ReceiptExtractor {
    enum class Country {
        RS,        // Serbia
        BA;        // Bosnia
        //HR,        // Croatia
        //AT,        // Austria

        fun toID(): Int {
            return when(this) {
                BA -> R.string.country_name_bosnia
                RS -> R.string.country_name_serbia
            }
        }
    }

    enum class MonetaryUnit {
        RSD,        // Serbia
        BAM;        // Bosnia

        companion object {
            fun fromStringOrThrow(str: String): MonetaryUnit {
                return entries.first { it.toString() == str }
            }
        }
    }

    enum class TransactionType {
        SALE,
        REFUND;

        companion object {
            fun fromTaxCoreNumberOrThrow(num: UByte): TransactionType {
                return when(num.toInt()) {
                    0 -> SALE
                    1 -> REFUND
                    else -> SALE // Invalid state, but safe fallback
                }
            }
        }
    }

    class InvalidURIHost(message: String) : Exception(message)
    class NotAnURI(message: String?) : Exception(message)
    class EntryAlreadyExists() : Exception()

    private var uri: URI
    private var country: Country

    constructor(data: ByteArray) {
        try {
            uri = URI.create(String(data, StandardCharsets.UTF_8))

            // The idea is if we have multiple Tax systems that we are able to determine which
            // one we pick from here
            country = when (uri.host) {
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
        val simple = when(this@ReceiptExtractor.country) {
            Country.RS -> TaxCore.SimpleReceipt(uri)
            Country.BA -> TaxCore.SimpleReceipt(uri)
        }

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
                is TaxCore.SimpleReceipt.FetchInvalidCertificate -> res.getString(R.string.ssl_exception, error.message)
                else -> res.getString(R.string.unknown_failure, error.message)
            }
        }

        fun getHumanAmount(amount: Long, unit: MonetaryUnit): Double {
            return when (unit) {
                MonetaryUnit.RSD -> amount.toDouble() / 10000f
                MonetaryUnit.BAM -> amount.toDouble() / 10000f
            }
        }
    }
}