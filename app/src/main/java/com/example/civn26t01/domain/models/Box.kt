package com.example.civn26t01.domain.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Box(
    @SerializedName("id")
    var id: Int = 0,

    @SerializedName("countInBox")
    var countInBox: Double = 0.0, // Changed from Int to Double

    @SerializedName("boxCount")
    var boxCount: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        countInBox = parcel.readDouble(), // Changed to readDouble()
        boxCount = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeDouble(countInBox) // Changed to writeDouble()
        parcel.writeInt(boxCount)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Box> {
        override fun createFromParcel(parcel: Parcel): Box {
            return Box(parcel)
        }

        override fun newArray(size: Int): Array<Box?> {
            return arrayOfNulls(size)
        }
    }
}