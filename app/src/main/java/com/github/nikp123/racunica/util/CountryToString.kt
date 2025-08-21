package com.github.nikp123.racunica.util

import com.github.nikp123.racunica.R

fun CountryToIDString(country: TaxCore.ReceiptExtractor.Country): Int {
    return when(country) {
        TaxCore.ReceiptExtractor.Country.BA -> R.string.country_name_bosnia
        TaxCore.ReceiptExtractor.Country.RS -> R.string.country_name_serbia
    }
}