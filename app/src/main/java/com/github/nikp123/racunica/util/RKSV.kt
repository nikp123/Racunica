package com.github.nikp123.racunica.util

import java.nio.charset.StandardCharsets
import com.github.nikp123.racunica.data.Receipt
import com.github.nikp123.racunica.data.ReceiptStatus
import com.github.nikp123.racunica.data.ReceiptStore
import com.github.nikp123.racunica.data.Store
import com.github.nikp123.racunica.data.StoreStatus
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlin.String

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RKSV {
    class SimpleReceipt {
        class BrokenData(message: String) : Exception(message)
        class DecodeError(message: String) : Exception(message)

        val storeID: String
        val receiptID: String
        val totalAmount: Long
        val timestamp: Long                    // 64bit UNIX epoch
        val transactionType: ReceiptExtractor.TransactionType

        // Custom values I've made for my self
        val unit:       ReceiptExtractor.MonetaryUnit
        val country:    ReceiptExtractor.Country
        val data:       ByteArray

        /**
         * @param data - Takes in the byte data of the receipt obtained from the QR code
         */
        constructor(data: ByteArray) {
            this.data = data

            val string = String(data, StandardCharsets.UTF_8)
            val parts = string.split("_")

            if(parts.size != 14 || parts[1].take(5) != "R1-AT") {
                throw BrokenData(
                    String.format("The QR code does not contain valid RKSV data")
                )
            }

            unit = ReceiptExtractor.MonetaryUnit.EUR
            country = ReceiptExtractor.Country.AT

            // We need to add all of these to get a total
            val standardRate = parts[5].replace(',', '.').toFloat()
            val reducedRate1 = parts[6].replace(',', '.').toFloat()
            val reducedRate2 = parts[7].replace(',', '.').toFloat()
            val zeroTax      = parts[8].replace(',', '.').toFloat()
            val specialTax   = parts[9].replace(',', '.').toFloat()

            try {
                // We need to add all the taxed amounts to get a total
                totalAmount = ((standardRate + reducedRate1 + reducedRate2 + zeroTax + specialTax) * 100).toLong()

                // Spec issue: The QR code stores timestamps in ULongs
                // but the correct UNIX timestamp should be Long
                timestamp = run {
                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    formatter.timeZone = TimeZone.getTimeZone("Europe/Vienna")
                    formatter.parse(parts[4]).time
                }

                receiptID = parts[3]
                storeID = parts[2]

                transactionType = ReceiptExtractor.TransactionType.SALE
            } catch(e: Exception) {
                throw DecodeError(e.message.toString())
            }
        }

        fun get(): ReceiptStore {
            return ReceiptStore(Receipt(
                id = 0,
                amount = this.totalAmount,
                unit = this.unit,
                time = this.timestamp,
                storeID = 0, // MUST be set later
                code  = this.receiptID,
                purchaserCode = null,
                status = ReceiptStatus.OFFLINE,
                type = this.transactionType,
                text = "",
                country = this.country,
                url = this.data.toString(),
                note = null,
            ),
            Store(
                status = StoreStatus.OFFLINE,
                code = storeID,
                name = storeID,
                usersName = null,
                country = this@SimpleReceipt.country,
                municipality = null,
                city         = null,
                address      = null,
                note         = null,
            ))
        }
    }
}
