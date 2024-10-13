package com.pcm.bluetoothsms.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pcm.bluetoothsms.R;
import com.pcm.bluetoothsms.Utills.HangingValue;
import com.pcm.bluetoothsms.fragment.ItemFragment;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link HangingValue} and makes a call to the
 * specified {@link ItemFragment.OnListFragmentInteractionListener}.
 */
public class ItemRecyclerViewAdapter extends RecyclerView.Adapter<ItemRecyclerViewAdapter.ViewHolder> {

    private final List<HangingValue> mValues;
    private final ItemFragment.OnListFragmentInteractionListener mListener;
    private Context context;

    public ItemRecyclerViewAdapter(Context context, List<HangingValue> items, ItemFragment.OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mNo.setText("" + (position + 1));
        holder.mTime.setText(mValues.get(position).getTime());
        holder.mContentView.setText(mValues.get(position).getMessage());

        if (0 == position) {
            holder.bottom.setVisibility(View.VISIBLE);
        } else holder.bottom.setVisibility(View.GONE);

        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                    builder.setTitle("Confirm to delete..?");
                    builder.setCancelable(false);

                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mListener.onListFragmentInteraction(holder.mItem);
                        }
                    });
                    builder.show();
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNo, mTime, mContentView, bottom;
        public HangingValue mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNo = (TextView) view.findViewById(R.id.id);
            mTime = (TextView) view.findViewById(R.id.time);
            mContentView = (TextView) view.findViewById(R.id.content);
            bottom = (TextView) view.findViewById(R.id.bottom);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
