/*
 * Copyright (C) 2009-2016 Johan Nilsson <http://markupartist.com>
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
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.data.models.Fare
import com.markupartist.sthlmtraveling.utils.Analytics.Companion.getInstance
import com.markupartist.sthlmtraveling.utils.IntentUtil.smsIntent

class TicketDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_ticket, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = getArguments()
        val fare = args!!.getParcelable<Fare?>(ARG_FARE)

        val recyclerView = view.findViewById<View?>(R.id.recycler_view) as RecyclerView
        recyclerView.setLayoutManager(LinearLayoutManager(getContext()))

        val adapter = TicketAdapter(getContext())
        for (fareAttribute in fare!!.attributes!!) {
            val description =
                getText(if (fareAttribute!!.isReduced) R.string.sms_ticket_price_reduced else R.string.sms_ticket_price_full)
            adapter.addTicket(
                TicketPrice(
                    description,
                    fare.zones,
                    fareAttribute.text,
                    fareAttribute.isReduced,
                    fareAttribute.action
                )
            )
        }

        recyclerView.setAdapter(adapter)
        ItemClickSupport.Companion.addTo(recyclerView)
            .setOnItemClickListener(object : ItemClickSupport.OnItemClickListener {
                override fun onItemClicked(recyclerView: RecyclerView?, position: Int, v: View?) {
                    val item = adapter.getItem(position)
                    when (item.viewType) {
                        TicketItem.Companion.PRICE_ITEM -> {
                            val ticketPrice = item.`object` as TicketPrice
                            val uri = Uri.parse(ticketPrice.action)
                            val action = uri.getQueryParameter("action")
                            if ("sms" == action) {
                                Companion.sendSms(
                                    getContext()!!, ticketPrice.isReduced,
                                    uri.getQueryParameter("number")!!,
                                    uri.getQueryParameter("message")!!
                                )
                            }
                        }

                        TicketItem.Companion.HEADER_ITEM -> {}
                    }
                }
            })
    }

    internal class TicketPrice(
        var description: CharSequence?,
        var zones: String?,
        var priceText: String?,
        var isReduced: Boolean,
        var action: String?
    ) {
        val price: CharSequence?
            get() = priceText
    }

    internal class TicketAdapter(private val mContext: Context?) :
        RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
        private val mTicketItems: MutableList<TicketItem<*>> = ArrayList<TicketItem<*>>()
        private val mLayoutInflater: LayoutInflater

        init {
            this.mLayoutInflater = LayoutInflater.from(mContext)
        }

        override fun getItemViewType(position: Int): Int {
            return mTicketItems.get(position).viewType
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            return when (viewType) {
                TicketItem.Companion.PRICE_ITEM -> PriceViewHolder.Companion.create(
                    mLayoutInflater,
                    parent
                )
                else -> throw IllegalArgumentException("Unknown view type: $viewType")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                TicketItem.Companion.PRICE_ITEM -> PriceViewHolder.Companion.bindTo(
                    holder as PriceViewHolder,
                    mTicketItems.get(position).`object` as TicketPrice
                )
            }
        }

        override fun getItemCount(): Int {
            return mTicketItems.size
        }

        fun addTicket(ticket: TicketPrice?) {
            mTicketItems.add(TicketItem<TicketPrice?>(ticket, TicketItem.Companion.PRICE_ITEM))
        }

        fun getItem(position: Int): TicketItem<*> {
            return mTicketItems.get(position)
        }
    }

    class TicketItem<T>(var `object`: T?, @field:ViewType var viewType: Int) {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(HEADER_ITEM, PRICE_ITEM)
        annotation class ViewType

        companion object {
            const val HEADER_ITEM: Int = 0
            const val PRICE_ITEM: Int = 1
        }
    }

    private class PriceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionTextView: TextView
        private val priceTextView: TextView
        private val zonesTextView: TextView

        init {
            descriptionTextView = itemView.findViewById<View?>(R.id.ticket_description) as TextView
            priceTextView = itemView.findViewById<View?>(R.id.ticket_price) as TextView
            zonesTextView = itemView.findViewById<View?>(R.id.ticket_zones) as TextView
        }

        companion object {
            fun create(inflater: LayoutInflater, parent: ViewGroup?): RecyclerView.ViewHolder {
                return PriceViewHolder(inflater.inflate(R.layout.item_ticket_price, parent, false))
            }

            fun bindTo(holder: PriceViewHolder, ticket: TicketPrice) {
                holder.descriptionTextView.setText(ticket.description)
                holder.priceTextView.setText(ticket.price)
                holder.zonesTextView.setText(ticket.zones)
            }
        }
    }

    companion object {
        const val ARG_FARE: String = "zones"

        @JvmStatic
        fun create(fare: Fare?): TicketDialogFragment {
            val b = Bundle()
            b.putParcelable(ARG_FARE, fare)
            val fragment = TicketDialogFragment()
            fragment.setArguments(b)
            return fragment
        }

        /**
         * Invokes the Messaging application.
         *
         * @param reducedPrice True if the price is reduced, false otherwise.
         * @param number       The number
         * @param message      The message
         */
        private fun sendSms(
            context: Context, reducedPrice: Boolean,
            number: String, message: String
        ) {
            val price = if (reducedPrice) "R" else "H" // Only kept for analytic purposes
            getInstance(context).event("Ticket", "Buy SMS Ticket", price)
            smsIntent(context, number, message)
        }
    }
}