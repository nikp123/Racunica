package com.github.nikp123.racunica.util

import android.annotation.SuppressLint
import com.github.nikp123.racunica.data.Receipt
import com.github.nikp123.racunica.data.ReceiptStatus
import com.github.nikp123.racunica.util.ReceiptExtractor.TransactionType
import java.net.URI
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlin.String
import kotlin.io.encoding.ExperimentalEncodingApi
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class HRPorezna {
    class SimpleReceipt {
        class BrokenURL(message: String) : Exception(message)
        class DecodeError(message: String) : Exception(message)

        val receiptID: String
        val totalAmount: Long
        val timestamp: Long                    // 64bit UNIX epoch
        val transactionType: ReceiptExtractor.TransactionType

        // Custom values I've made for my self
        val unit:       ReceiptExtractor.MonetaryUnit
        val uri:        URI
        val country:    ReceiptExtractor.Country

        /**
         * @param uri - Takes in the URL of the receipt obtained from the QR code
         */
        @SuppressLint("SimpleDateFormat")
        @OptIn(ExperimentalEncodingApi::class)
        constructor(uri: URI) {
            this.uri = uri

            unit = ReceiptExtractor.MonetaryUnit.EUR

            country = ReceiptExtractor.Country.HR

            if( uri.path != "/rn/" ) {
                throw BrokenURL(
                    String.format("The URL path doesn't seem right? Have they changed the receipt format???? Expected: '/rn/', got '%s'",
                        uri.path)
                )
            }

            val query = URIUtils(uri).parseQuery()

            if(query["jir"].isNullOrBlank() ||
                query["datv"].isNullOrBlank() ||
                query["izn"].isNullOrBlank()) {
                throw BrokenURL(
                    "The URL does not contain some or all of the known parameters!"
                )
            }

            val izn = query["izn"]?.replace(",", ".")
            val datv = query["datv"]
            val jir = query["jir"]

            try {
                totalAmount = (izn?.toFloat()?.times(100))?.toLong()!!

                // Spec issue: The QR code stores timestamps in ULongs
                // but the correct UNIX timestamp should be Long
                timestamp = run {
                    val formatter = SimpleDateFormat("yyyyMMdd_HHmm")
                    formatter.timeZone = TimeZone.getTimeZone("Europe/Zagreb")
                    formatter.parse(datv!!).time
                }

                receiptID = jir!!

                transactionType = TransactionType.SALE
            } catch(e: Exception) {
                throw DecodeError(e.message.toString())
            }
        }

        fun getReceipt(): Receipt {
            return Receipt(
                id = 0,
                amount = this.totalAmount,
                unit = this.unit,
                time = this.timestamp,
                storeID = null,
                code  = this.receiptID,
                purchaserCode = null,
                status = ReceiptStatus.OFFLINE,
                type = this.transactionType,
                text = "",
                country = this.country,
                url = uri.toString(),
                note = null,
            )
        }
    }
}

