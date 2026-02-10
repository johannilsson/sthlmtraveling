/*
 * Copyright (C) 2009-2015 Johan Nilsson <http://markupartist.com>
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
package com.markupartist.sthlmtraveling.ui.view

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import java.util.Arrays

/**
 * Based on https://gist.github.com/gabrielemariotti/4c189fb1124df4556058
 */
class SectionedRecyclerViewAdapter(
    private val context: Context?,
    @field:LayoutRes @param:LayoutRes private val sectionResourceId: Int,
    @field:IdRes @param:IdRes private val textResourceId: Int,
    private val baseAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var valid = true
    private val sections = SparseArray<Section?>()


    init {
        this.baseAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                valid = this@SectionedRecyclerViewAdapter.baseAdapter.getItemCount() > 0
                notifyDataSetChanged()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                valid = this@SectionedRecyclerViewAdapter.baseAdapter.getItemCount() > 0
                notifyItemRangeChanged(positionStart, itemCount)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                valid = this@SectionedRecyclerViewAdapter.baseAdapter.getItemCount() > 0
                notifyItemRangeInserted(positionStart, itemCount)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                valid = this@SectionedRecyclerViewAdapter.baseAdapter.getItemCount() > 0
                notifyItemRangeRemoved(positionStart, itemCount)
            }
        })
    }


    class SectionViewHolder(view: View, mTextResourceid: Int) : RecyclerView.ViewHolder(view) {
        var title: TextView

        init {
            title = view.findViewById<View?>(mTextResourceid) as TextView
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, typeView: Int): RecyclerView.ViewHolder {
        if (typeView == SECTION_TYPE) {
            val view = LayoutInflater.from(context).inflate(sectionResourceId, parent, false)
            return SectionViewHolder(view, textResourceId)
        } else {
            return baseAdapter.onCreateViewHolder(parent, typeView - 1)
        }
    }

    override fun onBindViewHolder(sectionViewHolder: RecyclerView.ViewHolder, position: Int) {
        if (isSectionHeaderPosition(position)) {
            (sectionViewHolder as SectionViewHolder).title.setText(sections.get(position)!!.title)
        } else {
            @Suppress("UNCHECKED_CAST")
            (baseAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>).onBindViewHolder(sectionViewHolder, sectionedPositionToPosition(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSectionHeaderPosition(position))
            SECTION_TYPE
        else
            baseAdapter.getItemViewType(sectionedPositionToPosition(position)) + 1
    }


    class Section(var firstPosition: Int, var title: CharSequence?) {
        var sectionedPosition: Int = 0
    }


    fun setSections(sections: Array<Section>) {
        this.sections.clear()

        Arrays.sort(sections, object : Comparator<Section> {
            override fun compare(o: Section, o1: Section): Int {
                return if (o.firstPosition == o1.firstPosition)
                    0
                else
                    (if (o.firstPosition < o1.firstPosition) -1 else 1)
            }
        })

        var offset = 0 // offset positions for the headers we're adding
        for (section in sections) {
            section.sectionedPosition = section.firstPosition + offset
            this.sections.append(section.sectionedPosition, section)
            ++offset
        }

        notifyDataSetChanged()
    }

    fun positionToSectionedPosition(position: Int): Int {
        var offset = 0
        for (i in 0..<sections.size()) {
            if (sections.valueAt(i)!!.firstPosition > position) {
                break
            }
            ++offset
        }
        return position + offset
    }

    fun sectionedPositionToPosition(sectionedPosition: Int): Int {
        if (isSectionHeaderPosition(sectionedPosition)) {
            return RecyclerView.NO_POSITION
        }

        var offset = 0
        for (i in 0..<sections.size()) {
            if (sections.valueAt(i)!!.sectionedPosition > sectionedPosition) {
                break
            }
            --offset
        }
        return sectionedPosition + offset
    }

    fun isSectionHeaderPosition(position: Int): Boolean {
        return sections.get(position) != null
    }


    override fun getItemId(position: Int): Long {
        return if (isSectionHeaderPosition(position))
            (
                    Int.Companion.MAX_VALUE - sections.indexOfKey(position)
                    ).toLong()
        else
            baseAdapter.getItemId(sectionedPositionToPosition(position))
    }

    override fun getItemCount(): Int {
        return (if (valid) baseAdapter.getItemCount() + sections.size() else 0)
    }

    companion object {
        private const val SECTION_TYPE = 0
    }
}