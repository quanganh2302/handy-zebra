package com.example.civn26t01.data.models

import android.os.Parcel
import android.os.Parcelable
import com.example.civn26t01.domain.models.Box
import com.google.gson.annotations.SerializedName

data class ChiyodaInfo(
    @SerializedName("wono")
    val wono: String = "",

    @SerializedName("completedCount")
    val completedCount: Double = 0.0, // Changed from Long to Double to support decimal values

    @SerializedName("entryDate")
    val entryDate: String? = null,

    @SerializedName("packingType")
    val packingType: Int = 0,

    @SerializedName("wonoComplete")
    val wonoComplete: Boolean = false,

    @SerializedName("listBox")
    val listBox: List<Box>? = null,

    @SerializedName("printer")
    val printer: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        wono = parcel.readString() ?: "",
        completedCount = parcel.readDouble(), // Changed to readDouble()
        entryDate = parcel.readString(),
        packingType = parcel.readInt(),
        wonoComplete = parcel.readByte().toInt() != 0,
        listBox = parcel.createTypedArrayList(Box.CREATOR),
        printer = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(wono)
        parcel.writeDouble(completedCount) // Changed to writeDouble()
        parcel.writeString(entryDate)
        parcel.writeInt(packingType)
        parcel.writeByte(if (wonoComplete) 1 else 0)
        parcel.writeTypedList(listBox)
        parcel.writeString(printer)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ChiyodaInfo> {
        override fun createFromParcel(parcel: Parcel): ChiyodaInfo {
            return ChiyodaInfo(parcel)
        }

        override fun newArray(size: Int): Array<ChiyodaInfo?> {
            return arrayOfNulls(size)
        }
    }
}