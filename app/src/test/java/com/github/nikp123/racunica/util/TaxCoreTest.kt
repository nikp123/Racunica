package com.github.nikp123.racunica.util

import com.github.nikp123.racunica.util.TaxCore.SimpleReceipt
import it.skrape.fetcher.BrowserFetcher
import org.junit.Test
import java.net.URI

class TaxCoreTest {
    @Test fun testFullDecodeRS() {
        if(true) {   // Test1
            val url =
                "https://suf.purs.gov.rs/v/?vl=A1JDUVJYQlI1UkNRUlhCUjWtVwMAC1QDADCFJwAAAAAAAAABi/HkkdQAAABBtDdO5vacnuZ1owLigYxPWWj54+xYqU9Q8N2pKmmBaOGrcfLRY10Z8Y9Gszt3ez/hkP4IPqzgZOdScMIaKIZrHAlYI6xnq/hDGJfNNImsdqXYm1rzQeniOW4/jCTgXS9HNr/XKGuKA6HCuO5K48bKkhPvIFvRhLfjcWMQAIwNApOXT1ob13uTfXJ0ZkOuluTmeoJXkEshfG8VNzgt3Um9Y20HY8sLCVxoyY+wdKUj9LO2PN0rLx4WM6KTnzasB4fWlOJ6CYLxF0LxHPxxeLqEg6X5AV5yfnhkyNm2eQgQXcMrApDzNfvrIGYJWpoEOKySg608HoKWkB4nYSfZMERMoHxIA2R7wU2Qmpds3lmRKWy0Rkqv3C79HCTIGuxkuUVzY4ACqIxbRheIHiExXNnImuCkMjt/oErHvXznGkQmtX0HMCz/ABfON5SMQzG5ZlqMKVFUR/6ofWmVp+rmJEwUSotp6R3V/Xwpkpz2pPP87SE2SYvP61l9B7JP7TwZgBk3Gset9X2zdFkHhAALcJAsxXTOuKg/tBkQ0h2TtumQuk0vImx3t5cUgyk8dXfFjK0/xxWlRXg2wiIXg5L8/XlV8PCcF1xZJ0PS/Ey5tUeFttsk5Lk9bN5Ey39FCA4NXrjy1MkQt70TV8m4euwkCt/lZUXGp73PZoSJXS9i7TnQHiI5XXCwhGdJ8E53/qULOq0="

            val receipt = SimpleReceipt(URI.create(url))

            assert(receipt.unit == ReceiptExtractor.MonetaryUnit.RSD)
            assert(receipt.requestedBy == "RCQRXBR5")
            assert(receipt.signedBy == "RCQRXBR5")
            assert(receipt.totalTransactions == 219053)
            assert(receipt.totalTransactionsOfType == 218123)
            assert((receipt.totalAmount / 10000) == 259.toLong())
            assert(receipt.timestamp == 1700570370516)
            assert(receipt.invoiceType == SimpleReceipt.InvoiceType.NORMAL)
            assert(receipt.transactionType == ReceiptExtractor.TransactionType.SALE)
            assert(receipt.buyerID == null)

            // TODO store test
            val (receipt_text, store) = receipt.fullScrape(BrowserFetcher).getOrThrow()
            val correct_receipt =
                "============ ФИСКАЛНИ РАЧУН ============\n" +
                "               100049486                \n" +
                "     dm drogerie markt doo Beograd      \n" +
                "          1138231-FILIJALA 070          \n" +
                "         НАРОДНОГ ФРОНТА 23Б            \n" +
                "                Нови Сад                \n" +
                "Касир:                            202702\n" +
                "ЕСИР број:                        962/23\n" +
                "-------------ПРОМЕТ ПРОДАЈА-------------\n" +
                "Артикли\n" +
                "========================================\n" +
                "Назив   Цена         Кол.         Укупно\n" +
                "DONTODENT zvake 50kom (Ђ)               \n" +
                "       259,00          1          259,00\n" +
                "----------------------------------------\n" +
                "Укупан износ:                     259,00\n" +
                "Платна картица:                   259,00\n" +
                "========================================\n" +
                "Ознака       Име      Стопа        Порез\n" +
                "Ђ           О-ПДВ   20,00%         43,17\n" +
                "----------------------------------------\n" +
                "Укупан износ пореза:               43,17\n" +
                "========================================\n" +
                "ПФР време:          21.11.2023. 13:39:30\n" +
                "ПФР број рачуна: RCQRXBR5-RCQRXBR5-21905\n" +
                "3                                       \n" +
                "Бројач рачуна:           218123/219053ПП\n" +
                "========================================\n" +
                "======== КРАЈ ФИСКАЛНОГ РАЧУНА =========";
            assert(receipt_text == correct_receipt)
        }

        if(true) {   // Test2
            val url =
                "https://suf.purs.gov.rs/v/?vl=A1AyR1FWR1FMUDJHUVZHUUzBCwEAeAsBAMAJDgAAAAAAAAABlsYgcOgAAABcbMVDqbIut7VkyShm6VvRT8Mw0T9ePDSRzm%2BzUCbJaQV4I6OOJsujB8YIHnYcp4yeWAVCUFSnVp4Z76Z0xVyQm%2FegcoqHxOs6mJwCc9zzuHZLTP2Rm6WPY6%2FxBtQtGIsFAdtZsCP9xcQNlm8zveYuyBcEcYNINGG6OMDpna%2F%2Bb2TZJcLzNod7zcgvEUdaY2Ut5A3TnAWQmCBjcD6eBdbzkXEwqQztRpe73SmOFKTO5NExdwxPX%2BkLGYZlTp1zVNMRC61GaI4Q2jf7olQ%2Fy4LDvXOVQz2m1zCtp7Z%2FNhd9aFfKnLCDudzbEqucmTVOG0j3sZCdFqv82f6tycHjw3snne0QtRZJuspj4CGe646tVqisei7LxoV3JuXi6EVFPlkhsl5%2B3ioI7kGDOZPcoGbmcp%2B%2FOZFCmagUPRp7sJTa2aKwxsxqXqJ3kuJIRZlqVbbEsewyk2yDKTEvYb1QC5ffVYw9FrRq1ChX%2FMKqJeW1T8Azr%2BKTXJes7eZx9DDCRTHXMYKPYZ%2BmGpHNppO%2F%2FijjLxrwb%2FODXQ0LFhbBqTvm2Jzp0oUjo7NyG0mo9mPN11axF8juHzC4OnY6uN0onk%2B9Oa0fVFqxlTQkYy0E5p%2FlONYB5UbvMddXoquA%2FDdtUqnvJnkYfajZdkCaxHN1dk4QbyQjXIVqKSAtPzzx25KAX8z97fyPK8HJE2x142ZJo60%3D"
            val receipt = SimpleReceipt(URI.create(url))

            assert(receipt.unit == ReceiptExtractor.MonetaryUnit.RSD)
            assert(receipt.requestedBy == "P2GQVGQL")
            assert(receipt.signedBy == "P2GQVGQL")
            assert(receipt.totalTransactions == 68545)
            assert(receipt.totalTransactionsOfType == 68472)
            assert((receipt.totalAmount / 10000) == 92.toLong())
            assert(receipt.timestamp == 1747080737000)
            assert(receipt.invoiceType == SimpleReceipt.InvoiceType.NORMAL)
            assert(receipt.transactionType == ReceiptExtractor.TransactionType.SALE)
            assert(receipt.buyerID == null)

            val (receipt_text, store) = receipt.fullScrape(BrowserFetcher).getOrThrow()
            val correct_receipt =
                "============ ФИСКАЛНИ РАЧУН ============\n" +
                "               106363820                \n" +
                "  MIKROMARKET NS DOO SREMSKA KAMENICA   \n" +
                "             1119077-ПЈ 110             \n" +
                "           СТРАЖИЛОВСКА 19              \n" +
                "                Нови Сад                \n" +
                "Касир:                                14\n" +
                "ЕСИР број:                      343/0001\n" +
                "-------------ПРОМЕТ ПРОДАЈА-------------\n" +
                "Артикли\n" +
                "========================================\n" +
                "Назив   Цена         Кол.         Укупно\n" +
                "PROFI.METRO COKOLADA 30GR #/KOM (Ђ)     \n" +
                "        46,00          2           92,00\n" +
                "----------------------------------------\n" +
                "Укупан износ:                      92,00\n" +
                "Готовина:                          92,00\n" +
                "========================================\n" +
                "Ознака       Име      Стопа        Порез\n" +
                "Ђ           О-ПДВ   20,00%         15,33\n" +
                "----------------------------------------\n" +
                "Укупан износ пореза:               15,33\n" +
                "========================================\n" +
                "ПФР време:          12.05.2025. 22:12:17\n" +
                "ПФР број рачуна: P2GQVGQL-P2GQVGQL-68545\n" +
                "Бројач рачуна:             68472/68545ПП\n" +
                "========================================\n" +
                "======== КРАЈ ФИСКАЛНОГ РАЧУНА =========";
            assert(receipt_text == correct_receipt)
        }
    }

    @Test fun testFullDecodeBiH() {
        if(true) {
            val url =
                "https://suf.poreskaupravars.org/v/?vl=A05KNEJFSDRITko0QkVINEikvwIAaLcCANhHAwAAAAAAAAABl5wIomAAAACy87rImxkBmysYi+mgmwswwcAN5Lfw6BSjY/g3YtkDOV5ikl0Xfnn4+q4K5eWIMYNlaeuYiLNYNXcKopA/MS48c7OqyUjtWX06Fuc4vXwLMVcRa8sDDB9iYAeLsj902yt5Ll69GoKvj+T6/nobhWxE1kY7D2Rn+az0+eJ7UcBLxtaLgV6W5Qi43W8NWuf1+65KPJRr/i5zO1iaRD4UIwrXg0g1a2MqoM2EqgUhqpqy4gtFA4JAau6BI/dhxzxiukjAWE1Zm0fxiuNWbvsTnFlyX/oI9Kzj0xpoVkoy/b9peclmv8+anXR4yelpEQJD8vFgTv8/BHt7Ysbqv1q2ZdwQIsKtuqV+RsXwULdsfDj2WQcj4Kcwjumhoo7TNDcdyBV8S7AwxHE+Oj9zy5OV1nPFY70HmQJ66m2zjiB790G9ZUGzRCgSWzQALu5sbXL2/e+lATf3ZAPyELd1DbmqGGxJqFVDW7hKSF/qUNasyvJVHhfOSRvdxkl7CSwm8gLmx4Q83DQGyqSoMww8QEQjICrC5hnPnG62e1z9QFtLcKzEdNkLgBELua6uwNLIb6TjfPxh0fofMJJvjGbidTbjWz50fQZM9moFu0ga/Fy1ouixNC96gb964IO40ev+mwTxsxEvnMaeRwPcBLvVrqI5Hmkso9FPFJZF1c9xsKktr3SczeWmNoa1l8R6BPMIuLnToX0="

            val receipt = SimpleReceipt(URI.create(url))

            assert(receipt.unit == ReceiptExtractor.MonetaryUnit.BAM)
            assert(receipt.requestedBy == "NJ4BEH4H")
            assert(receipt.signedBy == "NJ4BEH4H")
            assert(receipt.totalTransactions == 180132)
            assert(receipt.totalTransactionsOfType == 178024)
            assert((receipt.totalAmount.toFloat() / 10000) == 21.50f)
            assert(receipt.timestamp == 1750669501024)
            assert(receipt.invoiceType == SimpleReceipt.InvoiceType.NORMAL)
            assert(receipt.transactionType == ReceiptExtractor.TransactionType.SALE)
            assert(receipt.buyerID == null)

            // TODO store test
            val (receipt_text, store) = receipt.fullScrape(BrowserFetcher).getOrThrow()
            val correct_receipt =
                "============ ФИСКАЛНИ РАЧУН ============\n" +
                "             4403237830000              \n" +
                "    \"RESPECT PLUS\" d.o.o. Banja Luka    \n" +
                "10024403237830001-\"RESPECT PLUS\" d.o.o. \n" +
                "               Banja Luka               \n" +
                "       Ulica Braće Podgornika bb        \n" +
                "               Banja Luka               \n" +
                "Касир:                        Prodavac 1\n" +
                "ЕСИР број:                        77/4.0\n" +
                "-------------ПРОМЕТ ПРОДАЈА-------------\n" +
                "Артикли\n" +
                "========================================\n" +
                "Назив   Цијена       Кол.         Укупно\n" +
                "Prevoz do granice (Е)                   \n" +
                "         3,03          1            3,03\n" +
                "Prevoz od granice (К)                   \n" +
                "        16,97          1           16,97\n" +
                "Stanicna usluga (Е)                     \n" +
                "         1,00          1            1,00\n" +
                "Dodatno osiguranje (Е)                  \n" +
                "         0,50          1            0,50\n" +
                "----------------------------------------\n" +
                "Укупан износ:                      21,50\n" +
                "Готовина:                          21,50\n" +
                "========================================\n" +
                "Ознака       Име      Стопа        Порез\n" +
                "К         Без ПДВ    0,00%          0,00\n" +
                "Е             ПДВ   17,00%          0,66\n" +
                "----------------------------------------\n" +
                "Укупан износ пореза:                0,66\n" +
                "========================================\n" +
                "ПФР вријеме:         23.6.2025. 11:05:01\n" +
                "ПФР број рачуна: NJ4BEH4H-NJ4BEH4H-18013\n" +
                "2                                       \n" +
                "Бројач рачуна:           178024/180132ПП\n" +
                "========================================\n" +
                "======== КРАЈ ФИСКАЛНОГ РАЧУНА =========";

            assert(receipt_text == correct_receipt)
        }
    }
}