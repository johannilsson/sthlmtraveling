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

package com.markupartist.sthlmtraveling.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.AppConfig;
import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.IntentUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;


public class TicketDialogFragment extends BottomSheetDialogFragment {

    static final String ARG_ZONES = "zones";

    public static TicketDialogFragment create(String zones) {
        Bundle b = new Bundle();
        b.putString(ARG_ZONES, zones);
        TicketDialogFragment fragment = new TicketDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_ticket, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String zones = args.getString(ARG_ZONES);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        final TicketAdapter adapter = new TicketAdapter(getContext());
        adapter.addTicket(new TicketPrice(getText(R.string.sms_ticket_price_full), zones, false));
        adapter.addTicket(new TicketPrice(getText(R.string.sms_ticket_price_reduced), zones, true));
        recyclerView.setAdapter(adapter);
        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                TicketItem item = adapter.getItem(position);
                switch (item.viewType) {
                    case TicketItem.PRICE_ITEM:
                        TicketPrice ticketPrice = (TicketPrice) item.object;
                        sendSms(getContext(), ticketPrice.isReduced, ticketPrice.zones);
                        break;
                    case TicketItem.HEADER_ITEM:
                        // No-op
                        break;
                }
            }
        });
    }

    /**
     * Invokes the Messaging application.
     *
     * @param reducedPrice True if the price is reduced, false otherwise.
     */
    private static void sendSms(final Context context, final boolean reducedPrice, final String tariffZones) {
        String price = reducedPrice ? "R" : "H";
        Analytics.getInstance(context).event("Ticket", "Buy SMS Ticket", price);
        IntentUtil.smsIntent(context, AppConfig.TICKET_SMS_NUMBER, price + tariffZones);
    }

    static class TicketPrice {
        public String zones;
        public CharSequence description;
        public boolean isReduced;

        public TicketPrice(CharSequence description, String zones, boolean isReduced) {
            this.description = description;
            this.zones = zones;
            this.isReduced = isReduced;
        }

        public CharSequence getFullPrice() {
            return AppConfig.TICKET_FULL_PRICE[zones.length() - 1] + " kr";
        }

        public CharSequence getReducedPrice() {
            return AppConfig.TICKET_REDUCED_PRICE[zones.length() - 1] + " kr";
        }

        public CharSequence getPrice() {
            if (isReduced) {
                return getReducedPrice();
            }
            return getFullPrice();
        }
    }

    static class TicketAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<TicketItem> mTicketItems = new ArrayList<>();
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;

        public TicketAdapter(Context context) {
            this.mContext = context;
            this.mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getItemViewType(int position) {
            return mTicketItems.get(position).viewType;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TicketItem.PRICE_ITEM:
                    return PriceViewHolder.create(mLayoutInflater, parent);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case TicketItem.PRICE_ITEM:
                    PriceViewHolder.bindTo((PriceViewHolder) holder,
                            (TicketPrice) mTicketItems.get(position).object);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mTicketItems.size();
        }

        public void addTicket(TicketPrice ticket) {
            mTicketItems.add(new TicketItem<>(ticket, TicketItem.PRICE_ITEM));
        }

        public TicketItem getItem(int position) {
            return mTicketItems.get(position);
        }
    }

    public static class TicketItem<T> {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({HEADER_ITEM, PRICE_ITEM})
        public @interface ViewType {
        }

        public static final int HEADER_ITEM = 0;
        public static final int PRICE_ITEM = 1;

        public T object;
        @ViewType
        public int viewType;

        public TicketItem(T object, int viewType) {
            this.object = object;
            this.viewType = viewType;
        }
    }

    private static class PriceViewHolder extends RecyclerView.ViewHolder {
        private final TextView descriptionTextView;
        private final TextView priceTextView;
        private final TextView zonesTextView;

        public PriceViewHolder(View itemView) {
            super(itemView);
            descriptionTextView = (TextView) itemView.findViewById(R.id.ticket_description);
            priceTextView = (TextView) itemView.findViewById(R.id.ticket_price);
            zonesTextView = (TextView) itemView.findViewById(R.id.ticket_zones);
        }

        public static RecyclerView.ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new PriceViewHolder(inflater.inflate(R.layout.item_ticket_price, parent, false));
        }

        public static void bindTo(final PriceViewHolder holder, TicketPrice ticket) {
            holder.descriptionTextView.setText(ticket.description);
            holder.priceTextView.setText(ticket.getPrice());
            holder.zonesTextView.setText(ticket.zones);
        }
    }
}