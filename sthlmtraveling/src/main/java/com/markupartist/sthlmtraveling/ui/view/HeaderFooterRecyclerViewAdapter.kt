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

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Header & footer support for RecyclerView.Adapter.
 *
 * From, https://gist.github.com/mheras/0908873267def75dc746
 */
abstract class HeaderFooterRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var headerItemCount = 0
    private var contentItemCount = 0
    private var footerItemCount = 0

    /**
     * {@inheritDoc}
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Delegate to proper methods based on the viewType ranges.

        if (viewType >= HEADER_VIEW_TYPE_OFFSET && viewType < HEADER_VIEW_TYPE_OFFSET + VIEW_TYPE_MAX_COUNT) {
            return onCreateHeaderItemViewHolder(
                parent,
                viewType - HeaderFooterRecyclerViewAdapter.Companion.HEADER_VIEW_TYPE_OFFSET
            )!!
        } else if (viewType >= FOOTER_VIEW_TYPE_OFFSET && viewType < FOOTER_VIEW_TYPE_OFFSET + VIEW_TYPE_MAX_COUNT) {
            return onCreateFooterItemViewHolder(parent, viewType - FOOTER_VIEW_TYPE_OFFSET)
        } else if (viewType >= CONTENT_VIEW_TYPE_OFFSET && viewType < CONTENT_VIEW_TYPE_OFFSET + VIEW_TYPE_MAX_COUNT) {
            return onCreateContentItemViewHolder(parent, viewType - CONTENT_VIEW_TYPE_OFFSET)
        } else {
            // This shouldn't happen as we check that the viewType provided by the client is valid.
            throw IllegalStateException()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        // Delegate to proper methods based on the viewType ranges.
        if (headerItemCount > 0 && position < headerItemCount) {
            onBindHeaderItemViewHolder(viewHolder, position)
        } else if (contentItemCount > 0 && position - headerItemCount < contentItemCount) {
            onBindContentItemViewHolder(viewHolder, position - headerItemCount)
        } else {
            onBindFooterItemViewHolder(viewHolder, position - headerItemCount - contentItemCount)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun getItemCount(): Int {
        // Cache the counts and return the sum of them.
        headerItemCount = getHeaderItemCount()
        contentItemCount = getContentItemCount()
        footerItemCount = getFooterItemCount()
        return headerItemCount + contentItemCount + footerItemCount
    }

    /**
     * {@inheritDoc}
     */
    override fun getItemViewType(position: Int): Int {
        // Delegate to proper methods based on the position, but validate first.
        if (headerItemCount > 0 && position < headerItemCount) {
            return validateViewType(getHeaderItemViewType(position)) + HEADER_VIEW_TYPE_OFFSET
        } else if (contentItemCount > 0 && position - headerItemCount < contentItemCount) {
            return validateViewType(getContentItemViewType(position - headerItemCount)) + CONTENT_VIEW_TYPE_OFFSET
        } else {
            return validateViewType(getFooterItemViewType(position - headerItemCount - contentItemCount)) + FOOTER_VIEW_TYPE_OFFSET
        }
    }

    /**
     * Validates that the view type is within the valid range.
     *
     * @param viewType the view type.
     * @return the given view type.
     */
    private fun validateViewType(viewType: Int): Int {
        check(!(viewType < 0 || viewType >= VIEW_TYPE_MAX_COUNT)) { "viewType must be between 0 and " + VIEW_TYPE_MAX_COUNT }
        return viewType
    }

    /**
     * Notifies that a header item is inserted.
     *
     * @param position the position of the header item.
     */
    fun notifyHeaderItemInserted(position: Int) {
        val newHeaderItemCount = getHeaderItemCount()
        if (position < 0 || position >= newHeaderItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for header items [0 - " + (newHeaderItemCount - 1) + "].")
        }
        notifyItemInserted(position)
    }

    /**
     * Notifies that multiple header items are inserted.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyHeaderItemRangeInserted(positionStart: Int, itemCount: Int) {
        val newHeaderItemCount = getHeaderItemCount()
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > newHeaderItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for header items [0 - " + (newHeaderItemCount - 1) + "].")
        }
        notifyItemRangeInserted(positionStart, itemCount)
    }

    /**
     * Notifies that a header item is changed.
     *
     * @param position the position.
     */
    fun notifyHeaderItemChanged(position: Int) {
        if (position < 0 || position >= headerItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for header items [0 - " + (headerItemCount - 1) + "].")
        }
        notifyItemChanged(position)
    }

    /**
     * Notifies that multiple header items are changed.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyHeaderItemRangeChanged(positionStart: Int, itemCount: Int) {
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount >= headerItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for header items [0 - " + (headerItemCount - 1) + "].")
        }
        notifyItemRangeChanged(positionStart, itemCount)
    }


    /**
     * Notifies that an existing header item is moved to another position.
     *
     * @param fromPosition the original position.
     * @param toPosition   the new position.
     */
    fun notifyHeaderItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= headerItemCount || toPosition >= headerItemCount) {
            throw IndexOutOfBoundsException("The given fromPosition " + fromPosition + " or toPosition " + toPosition + " is not within the position bounds for header items [0 - " + (headerItemCount - 1) + "].")
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    /**
     * Notifies that a header item is removed.
     *
     * @param position the position.
     */
    fun notifyHeaderItemRemoved(position: Int) {
        if (position < 0 || position >= headerItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for header items [0 - " + (headerItemCount - 1) + "].")
        }
        notifyItemRemoved(position)
    }

    /**
     * Notifies that multiple header items are removed.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyHeaderItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > headerItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for header items [0 - " + (headerItemCount - 1) + "].")
        }
        notifyItemRangeRemoved(positionStart, itemCount)
    }

    /**
     * Notifies that a content item is inserted.
     *
     * @param position the position of the content item.
     */
    fun notifyContentItemInserted(position: Int) {
        val newHeaderItemCount = getHeaderItemCount()
        val newContentItemCount = getContentItemCount()
        if (position < 0 || position >= newContentItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for content items [0 - " + (newContentItemCount - 1) + "].")
        }
        notifyItemInserted(position + newHeaderItemCount)
    }

    /**
     * Notifies that multiple content items are inserted.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyContentItemRangeInserted(positionStart: Int, itemCount: Int) {
        val newHeaderItemCount = getHeaderItemCount()
        val newContentItemCount = getContentItemCount()
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > newContentItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for content items [0 - " + (newContentItemCount - 1) + "].")
        }
        notifyItemRangeInserted(positionStart + newHeaderItemCount, itemCount)
    }

    /**
     * Notifies that a content item is changed.
     *
     * @param position the position.
     */
    fun notifyContentItemChanged(position: Int) {
        if (position < 0 || position >= contentItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for content items [0 - " + (contentItemCount - 1) + "].")
        }
        notifyItemChanged(position + headerItemCount)
    }

    /**
     * Notifies that multiple content items are changed.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyContentItemRangeChanged(positionStart: Int, itemCount: Int) {
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > contentItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for content items [0 - " + (contentItemCount - 1) + "].")
        }
        notifyItemRangeChanged(positionStart + headerItemCount, itemCount)
    }

    /**
     * Notifies that an existing content item is moved to another position.
     *
     * @param fromPosition the original position.
     * @param toPosition   the new position.
     */
    fun notifyContentItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= contentItemCount || toPosition >= contentItemCount) {
            throw IndexOutOfBoundsException("The given fromPosition " + fromPosition + " or toPosition " + toPosition + " is not within the position bounds for content items [0 - " + (contentItemCount - 1) + "].")
        }
        notifyItemMoved(fromPosition + headerItemCount, toPosition + headerItemCount)
    }

    /**
     * Notifies that a content item is removed.
     *
     * @param position the position.
     */
    fun notifyContentItemRemoved(position: Int) {
        if (position < 0 || position >= contentItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for content items [0 - " + (contentItemCount - 1) + "].")
        }
        notifyItemRemoved(position + headerItemCount)
    }

    /**
     * Notifies that multiple content items are removed.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyContentItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > contentItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for content items [0 - " + (contentItemCount - 1) + "].")
        }
        notifyItemRangeRemoved(positionStart + headerItemCount, itemCount)
    }

    /**
     * Notifies that a footer item is inserted.
     *
     * @param position the position of the content item.
     */
    fun notifyFooterItemInserted(position: Int) {
        val newHeaderItemCount = getHeaderItemCount()
        val newContentItemCount = getContentItemCount()
        val newFooterItemCount = getFooterItemCount()
        if (position < 0 || position >= newFooterItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for footer items [0 - " + (newFooterItemCount - 1) + "].")
        }
        notifyItemInserted(position + newHeaderItemCount + newContentItemCount)
    }

    /**
     * Notifies that multiple footer items are inserted.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyFooterItemRangeInserted(positionStart: Int, itemCount: Int) {
        val newHeaderItemCount = getHeaderItemCount()
        val newContentItemCount = getContentItemCount()
        val newFooterItemCount = getFooterItemCount()
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > newFooterItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for footer items [0 - " + (newFooterItemCount - 1) + "].")
        }
        notifyItemRangeInserted(positionStart + newHeaderItemCount + newContentItemCount, itemCount)
    }

    /**
     * Notifies that a footer item is changed.
     *
     * @param position the position.
     */
    fun notifyFooterItemChanged(position: Int) {
        if (position < 0 || position >= footerItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for footer items [0 - " + (footerItemCount - 1) + "].")
        }
        notifyItemChanged(position + headerItemCount + contentItemCount)
    }

    /**
     * Notifies that multiple footer items are changed.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyFooterItemRangeChanged(positionStart: Int, itemCount: Int) {
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > footerItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for footer items [0 - " + (footerItemCount - 1) + "].")
        }
        notifyItemRangeChanged(positionStart + headerItemCount + contentItemCount, itemCount)
    }

    /**
     * Notifies that an existing footer item is moved to another position.
     *
     * @param fromPosition the original position.
     * @param toPosition   the new position.
     */
    fun notifyFooterItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= footerItemCount || toPosition >= footerItemCount) {
            throw IndexOutOfBoundsException("The given fromPosition " + fromPosition + " or toPosition " + toPosition + " is not within the position bounds for footer items [0 - " + (footerItemCount - 1) + "].")
        }
        notifyItemMoved(
            fromPosition + headerItemCount + contentItemCount,
            toPosition + headerItemCount + contentItemCount
        )
    }

    /**
     * Notifies that a footer item is removed.
     *
     * @param position the position.
     */
    fun notifyFooterItemRemoved(position: Int) {
        if (position < 0 || position >= footerItemCount) {
            throw IndexOutOfBoundsException("The given position " + position + " is not within the position bounds for footer items [0 - " + (footerItemCount - 1) + "].")
        }
        notifyItemRemoved(position + headerItemCount + contentItemCount)
    }

    /**
     * Notifies that multiple footer items are removed.
     *
     * @param positionStart the position.
     * @param itemCount     the item count.
     */
    fun notifyFooterItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (positionStart < 0 || itemCount < 0 || positionStart + itemCount > footerItemCount) {
            throw IndexOutOfBoundsException("The given range [" + positionStart + " - " + (positionStart + itemCount - 1) + "] is not within the position bounds for footer items [0 - " + (footerItemCount - 1) + "].")
        }
        notifyItemRangeRemoved(positionStart + headerItemCount + contentItemCount, itemCount)
    }

    /**
     * Gets the header item view type. By default, this method returns 0.
     *
     * @param position the position.
     * @return the header item view type (within the range [0 - VIEW_TYPE_MAX_COUNT-1]).
     */
    protected fun getHeaderItemViewType(position: Int): Int {
        return 0
    }

    /**
     * Gets the footer item view type. By default, this method returns 0.
     *
     * @param position the position.
     * @return the footer item view type (within the range [0 - VIEW_TYPE_MAX_COUNT-1]).
     */
    protected fun getFooterItemViewType(position: Int): Int {
        return 0
    }

    /**
     * Gets the content item view type. By default, this method returns 0.
     *
     * @param position the position.
     * @return the content item view type (within the range [0 - VIEW_TYPE_MAX_COUNT-1]).
     */
    protected fun getContentItemViewType(position: Int): Int {
        return 0
    }

    /**
     * Gets the header item count. This method can be called several times, so it should not calculate the count every time.
     *
     * @return the header item count.
     */
    protected abstract fun getHeaderItemCount(): Int

    /**
     * Gets the footer item count. This method can be called several times, so it should not calculate the count every time.
     *
     * @return the footer item count.
     */
    protected abstract fun getFooterItemCount(): Int

    /**
     * Gets the content item count. This method can be called several times, so it should not calculate the count every time.
     *
     * @return the content item count.
     */
    protected abstract fun getContentItemCount(): Int

    /**
     * This method works exactly the same as [.onCreateViewHolder], but for header items.
     *
     * @param parent         the parent view.
     * @param headerViewType the view type for the header.
     * @return the view holder.
     */
    protected abstract fun onCreateHeaderItemViewHolder(
        parent: ViewGroup,
        headerViewType: Int
    ): RecyclerView.ViewHolder

    /**
     * This method works exactly the same as [.onCreateViewHolder], but for footer items.
     *
     * @param parent         the parent view.
     * @param footerViewType the view type for the footer.
     * @return the view holder.
     */
    protected abstract fun onCreateFooterItemViewHolder(
        parent: ViewGroup,
        footerViewType: Int
    ): RecyclerView.ViewHolder

    /**
     * This method works exactly the same as [.onCreateViewHolder], but for content items.
     *
     * @param parent          the parent view.
     * @param contentViewType the view type for the content.
     * @return the view holder.
     */
    protected abstract fun onCreateContentItemViewHolder(
        parent: ViewGroup,
        contentViewType: Int
    ): RecyclerView.ViewHolder

    /**
     * This method works exactly the same as [.onBindViewHolder], but for header items.
     *
     * @param headerViewHolder the view holder for the header item.
     * @param position         the position.
     */
    protected abstract fun onBindHeaderItemViewHolder(
        headerViewHolder: RecyclerView.ViewHolder,
        position: Int
    )

    /**
     * This method works exactly the same as [.onBindViewHolder], but for footer items.
     *
     * @param footerViewHolder the view holder for the footer item.
     * @param position         the position.
     */
    protected abstract fun onBindFooterItemViewHolder(
        footerViewHolder: RecyclerView.ViewHolder,
        position: Int
    )

    /**
     * This method works exactly the same as [.onBindViewHolder], but for content items.
     *
     * @param contentViewHolder the view holder for the content item.
     * @param position          the position.
     */
    protected abstract fun onBindContentItemViewHolder(
        contentViewHolder: RecyclerView.ViewHolder,
        position: Int
    )

    companion object {
        private const val VIEW_TYPE_MAX_COUNT = 1000
        private const val HEADER_VIEW_TYPE_OFFSET = 0
        private val FOOTER_VIEW_TYPE_OFFSET: Int = HEADER_VIEW_TYPE_OFFSET + VIEW_TYPE_MAX_COUNT
        private val CONTENT_VIEW_TYPE_OFFSET: Int = FOOTER_VIEW_TYPE_OFFSET + VIEW_TYPE_MAX_COUNT
    }
}