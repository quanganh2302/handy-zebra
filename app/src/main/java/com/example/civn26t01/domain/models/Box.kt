package com.example.civn26t01.domain.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Box(
    @SerializedName("numberBox")
    var numberBox: Int = 0,

    @SerializedName("count")
    var count: Long = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        numberBox = parcel.readInt(),
        count = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(numberBox)
        parcel.writeLong(count)
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