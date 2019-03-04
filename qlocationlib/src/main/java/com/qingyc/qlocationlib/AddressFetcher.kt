package com.domobile.hindicalendar.modul.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import java.util.*

object AddressFetcher {

    fun getAddress(context: Context, locale: Locale?, location: Location): Address? {
        locale?.let {
            val geocoder = Geocoder(context, locale)
            val fromLocation = geocoder.getFromLocation(location.latitude, location.longitude, 3)
            if (fromLocation.isNullOrEmpty()) {
                return null
            }
            return fromLocation[0]
        } ?: let {
            return null
        }
    }

    fun getCity(context: Context, locale: Locale?, location: Location): String {
        val address = getAddress(context, locale, location)
        return address?.locality ?: ""
    }

}