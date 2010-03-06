package com.markupartist.sthlmtraveling.provider.deviation;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

public class Deviation implements Parcelable {
    private Long mReference;
    private int mMessageVersion;
    private String mLink;
    private String mMobileLink;
    private Time mCreated;
    private boolean mIsMainNews;
    private int mSortOrder;
    private String mHeader;
    private String mDetails;
    private String mScope;
    private String mScopeElements;

    public Deviation(Parcel parcel) {
        mReference = parcel.readLong();
        mMessageVersion = parcel.readInt();
        mLink = parcel.readString();
        mMobileLink = parcel.readString();
        mCreated = new Time();
        mCreated.parse3339(parcel.readString());
        mIsMainNews = (Boolean) parcel.readValue(null);
        mSortOrder = parcel.readInt();
        mHeader = parcel.readString();
        mDetails = parcel.readString();
        mScope = parcel.readString();
        mScopeElements = parcel.readString();
    }

    public Deviation() {
    }

    public void setReference(Long reference) {
        this.mReference = reference;
    }
    public Long getReference() {
        return mReference;
    }
    public void setMessageVersion(int messageVersion) {
        this.mMessageVersion = messageVersion;
    }
    public int getMessageVersion() {
        return mMessageVersion;
    }
    public void setMobileLink(String mobileLink) {
        this.mMobileLink = mobileLink;
    }
    public String getMobileLink() {
        return mMobileLink;
    }
    public void setCreated(Time created) {
        this.mCreated = created;
    }
    public Time getCreated() {
        return mCreated;
    }
    public void setMainNews(boolean isMainNews) {
        this.mIsMainNews = isMainNews;
    }
    public boolean isMainNews() {
        return mIsMainNews;
    }
    public void setSortOrder(int sortOrder) {
        this.mSortOrder = sortOrder;
    }
    public int getSortOrder() {
        return mSortOrder;
    }
    public void setLink(String mLink) {
        this.mLink = mLink;
    }
    public String getLink() {
        return mLink;
    }
    public void setHeader(String header) {
        this.mHeader = header;
    }
    public String getHeader() {
        return mHeader;
    }
    public void setDetails(String details) {
        this.mDetails = details;    
    }
    public String getDetails() {
        return mDetails;
    }
    public void setScope(String scope) {
        this.mScope = scope;
    }
    public String getScope() {
        return mScope;
    }
    public void setScopeElements(String scopeElements) {
        this.mScopeElements = scopeElements;
    }
    public String getScopeElements() {
        return mScopeElements;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mReference);
        parcel.writeInt(mMessageVersion);
        parcel.writeString(mLink);
        parcel.writeString(mMobileLink);
        parcel.writeString(mCreated.format3339(false));
        parcel.writeValue(mIsMainNews);
        parcel.writeInt(mSortOrder);
        parcel.writeString(mHeader);
        parcel.writeString(mDetails);
        parcel.writeString(mScope);
        parcel.writeString(mScopeElements);
    }

    public static final Creator<Deviation> CREATOR = new Creator<Deviation>() {
        public Deviation createFromParcel(Parcel parcel) {
            return new Deviation(parcel);
        }

        public Deviation[] newArray(int size) {
            return new Deviation[size];
        }
    };
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Deviation [mCreated=" + mCreated + ", mDetails=" + mDetails
                + ", mHeader=" + mHeader + ", mIsMainNews=" + mIsMainNews
                + ", mLink=" + mLink + ", mMessageVersion=" + mMessageVersion
                + ", mMobileLink=" + mMobileLink + ", mReference=" + mReference
                + ", mScope=" + mScope + ", mSortOrder=" + mSortOrder
                + ", sScopeElements=" + mScopeElements + "]";
    }
}
