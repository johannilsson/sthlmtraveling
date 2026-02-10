package com.markupartist.sthlmtraveling.provider.deviation

import android.os.Parcel
import android.os.Parcelable
import android.text.format.Time

class Deviation : Parcelable {
    private var mReference: Long? = null
    var messageVersion: Int = 0
    var link: String? = null
    var mobileLink: String? = null
    private var mCreated: Time? = null
    var isMainNews: Boolean = false
    var sortOrder: Int = 0
    var header: String? = null
    var details: String? = null
    var scope: String? = null
    var scopeElements: String? = null

    constructor(parcel: Parcel) {
        mReference = parcel.readLong()
        this.messageVersion = parcel.readInt()
        this.link = parcel.readString()
        this.mobileLink = parcel.readString()
        mCreated = Time()
        mCreated!!.parse3339(parcel.readString())
        this.isMainNews = (parcel.readValue(null) as kotlin.Boolean?)!!
        this.sortOrder = parcel.readInt()
        this.header = parcel.readString()
        this.details = parcel.readString()
        this.scope = parcel.readString()
        this.scopeElements = parcel.readString()
    }

    constructor()

    var reference: Long
        get() = mReference!!
        set(reference) {
            this.mReference = reference
        }
    var created: Time?
        get() = mCreated
        set(created) {
            this.mCreated = created
        }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(mReference!!)
        parcel.writeInt(this.messageVersion)
        parcel.writeString(this.link)
        parcel.writeString(this.mobileLink)
        parcel.writeString(mCreated!!.format3339(false))
        parcel.writeValue(this.isMainNews)
        parcel.writeInt(this.sortOrder)
        parcel.writeString(this.header)
        parcel.writeString(this.details)
        parcel.writeString(this.scope)
        parcel.writeString(this.scopeElements)
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        return ("Deviation [mCreated=" + mCreated + ", mDetails=" + this.details
                + ", mHeader=" + this.header + ", mIsMainNews=" + this.isMainNews
                + ", mLink=" + this.link + ", mMessageVersion=" + this.messageVersion
                + ", mMobileLink=" + this.mobileLink + ", mReference=" + mReference
                + ", mScope=" + this.scope + ", mSortOrder=" + this.sortOrder
                + ", sScopeElements=" + this.scopeElements + "]")
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Deviation?> = object : Parcelable.Creator<Deviation?> {
            override fun createFromParcel(parcel: Parcel): Deviation {
                return Deviation(parcel)
            }

            override fun newArray(size: Int): Array<Deviation?> {
                return arrayOfNulls<Deviation>(size)
            }
        }
    }
}
