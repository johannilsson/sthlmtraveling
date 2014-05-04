/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markupartist.sthlmtraveling;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Adapter that represent the data of several other adapters. 
 * 
 * Works well with the SeparatedListAdapter created by Jeffrey Sharkey that 
 * inspired me to create this one. http://jsharkey.org/blog/2008/08/18/separating-lists-with-headers-in-android-09/
 * 
 * Use it like.
 * <pre>
 *      mMultipleAdapter = new MultipleListAdapter();
 *      mMultipleAdapter.addAdapter(ADAPTER_FIRST, mFirstAdater);
 *      mMultipleAdapter.addAdapter(ADAPTER_SECOND, mSecondAdapter);
 *      mMultipleAdapter.addAdapter(ADAPTER_THIRD, mThird);
 * </pre>
 * @author johan
 */
public class MultipleListAdapter extends BaseAdapter {

    /**
     * The Log TAG.
     */
    @SuppressWarnings("unused")
    private static final String TAG = "MultipleListAdapter";

    /**
     * Represents all adapters.
     */
    private HashMap<Integer, BaseAdapter> mAdapters = 
        new HashMap<Integer, BaseAdapter>();

    /**
     * Lock used when adding new adapters.
     */
    private final Object mLock = new Object();

    /**
     * Get all adapters that that is handled by this adapter.
     * @return Adapters added to this adapter.
     */
    public HashMap<Integer, BaseAdapter> getAdapters() {
        return mAdapters;
    }

    /**
     * Adds a new adapter to the list.
     * @param adapter The adapter to add.
     */
    public void addAdapter(int id, BaseAdapter adapter) {
        synchronized (mLock) {
            mAdapters.put(id, adapter);
        }
    }

    /**
     * Get the adaper id present at the specifed position. 
     * <p>
     * Note. Pass the position that is relative to the current adapter. This is
     * important if used with nested adapters.
     * @param position The position.
     * @return The adapter id.
     */
    public int getAdapterId(int position) {
        for (Entry<Integer, BaseAdapter> e : mAdapters.entrySet()) {
            if (position == 0)
                return e.getKey();

            int size = e.getValue().getCount();

            if (position < size)
                return e.getKey();

            position -= size;
        }
        return -1;        
    }

    /**
     * How many items are in the data set representing this adpater and all
     * the underlying adpaters.
     */
    @Override
    public int getCount() {
        int total = 0;

        for (BaseAdapter adapter: mAdapters.values())
            total += adapter.getCount();

        return total;
    }

    /**
     * Converts the specified position into the adapters actual position.
     * @param position Index of the item
     * @return The position of the item whose data we want within the adapter's 
     *         data set.
     */
    public int getAdapterPosition(int position) {
        int sectionIndex = 0;

        for (BaseAdapter adapter : mAdapters.values()) {
            if (position == 0)
                return position;

            int size = adapter.getCount();

            if (position < size)
                return position;

            position -= size;
            sectionIndex++;
        }

        return sectionIndex;
    }

    /**
     * The adapter that is represented at the specified position.
     * @param position Index of the item.
     * @return The adapter.
     */
    public BaseAdapter getAdapter(int position) {
        for (BaseAdapter adapter: mAdapters.values()) {
            if (position == 0)
                return adapter;
            
            int size = adapter.getCount();

            if (position < size)
                return adapter;

            position -= size;
        }
        return null;
    }

    /**
     * Get the data item associated with the specified position in the data set.
     * Note. this will return the item that is represented by the current 
     * adapters position.
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        for (BaseAdapter adapter: mAdapters.values()) {
            if (position == 0)
                return adapter.getItem(position);

            int size = adapter.getCount();

            if (position < size)
                return adapter.getItem(position);

            position -= size;
        }

        return null;
    }

    /**
     * Get the item id id represented by the specified position.
     * Currently only returns the position. Might change to return the actual
     * item id.
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        int sectionIndex = 0;

        for (BaseAdapter adapter: mAdapters.values()) {
            if (position == 0)
                return adapter.getItemId(position);

            int size = adapter.getCount();

            if (position < size)
                return adapter.getItemId(position);

            position -= size;
            sectionIndex++;
        }

        return position;
    }

    /**
     * Returns the number of types of view types. This is the total sum of all
     * the added adapters.
     * @see android.widget.BaseAdapter#getViewTypeCount()
     */
    @Override
    public int getViewTypeCount() {
        int total = 0;

        for (BaseAdapter adapter: mAdapters.values())
            total += adapter.getViewTypeCount();

        return total;
    }

    /**
     * The view type that will be used when calling getView.
     * @see android.widget.BaseAdapter#getItemViewType(int)
     */
    @Override
    public int getItemViewType(int position) {
        int typeOffset = 0; // start counting from here

        for (BaseAdapter adapter : mAdapters.values()) {
            int size = adapter.getCount();

            if (position < size) {
                return typeOffset + adapter.getItemViewType(position);
            }

            position -= size;
            typeOffset += adapter.getViewTypeCount();
        }

        return -1;
    }
    
    /**
     * The View that is represented and the specified position.
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {        
        int sectionIndex = 0;

        for (BaseAdapter adapter: mAdapters.values()) {
            if (position == 0)
                return adapter.getView(position, convertView, parent);
            
            int size = adapter.getCount();

            if (position < size)
                return adapter.getView(position, convertView, parent);

            position -= size;
            sectionIndex++;
        }

        return null;
    }

    /**
     * Returns true if the item at the specified position is not a separator. 
     * (A separator is a non-selectable, non-clickable item).
     * @see android.widget.BaseAdapter#isEnabled(int)
     */
    @Override
    public boolean isEnabled(int position) {
        for (BaseAdapter adapter: mAdapters.values()) {
            if (position == 0)
                return adapter.isEnabled(position);
             
            int size = adapter.getCount();

            if (position < size)
                return adapter.isEnabled(position);

            position -= size;
        }
        return isEnabled(position);
    }
}
