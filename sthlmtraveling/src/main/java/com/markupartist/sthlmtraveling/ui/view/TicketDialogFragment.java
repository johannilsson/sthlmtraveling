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
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.data.models.Fare;
import com.markupartist.sthlmtraveling.data.models.FareAttribute;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.IntentUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;


public class TicketDialogFragment extends BottomSheetDialogFragment {

    static final String ARG_FARE = "zones";

    public static TicketDialogFragment create(Fare fare) {
        Bundle b = new Bundle();
        b.putParcelable(ARG_FARE, fare);
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
        Fare fare = args.getParcelable(ARG_FARE);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        final TicketAdapter adapter = new TicketAdapter(getContext());
        for (FareAttribute fareAttribute : fare.getAttributes()) {
            CharSequence description = getText(fareAttribute.isReduced() ?
                    R.string.sms_ticket_price_reduced :
                    R.string.sms_ticket_price_full);
            adapter.addTicket(new TicketPrice(description,
                    fare.getZones(),
                    fareAttribute.getText(),
                    fareAttribute.isReduced(),
                    fareAttribute.getAction()));
        }

        recyclerView.setAdapter(adapter);
        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                TicketItem item = adapter.getItem(position);
                switch (item.viewType) {
                    case TicketItem.PRICE_ITEM:
                        TicketPrice ticketPrice = (TicketPrice) item.object;
                        Uri uri = Uri.parse(ticketPrice.getAction());
                        String action = uri.getQueryParameter("action");
                        if ("sms".equals(action)) {
                            sendSms(getContext(), ticketPrice.isReduced,
                                    uri.getQueryParameter("number"),
                                    uri.getQueryParameter("message"));
                        }
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
     * @param number       The number
     * @param message      The message
     */
    private static void sendSms(final Context context, final boolean reducedPrice,
                                final String number, final String message) {
        String price = reducedPrice ? "R" : "H"; // Only kept for analytic purposes
        Analytics.getInstance(context).event("Ticket", "Buy SMS Ticket", price);
        IntentUtil.smsIntent(context, number, message);
    }

    static class TicketPrice {
        public String zones;
        public CharSequence description;
        public boolean isReduced;
        public String priceText;
        public String action;

        public TicketPrice(CharSequence description,
                           String zones,
                           String priceText,
                           boolean isReduced,
                           String action) {
            this.description = description;
            this.zones = zones;
            this.priceText = priceText;
            this.isReduced = isReduced;
            this.action = action;
        }

        public CharSequence getPrice() {
            return priceText;
        }

        public String getAction() {
            return action;
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