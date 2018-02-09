package hu.ait.android.runlogger.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import hu.ait.android.runlogger.R;
import hu.ait.android.runlogger.data.Run;


/**
 * Created by Gilmourj on 11/23/17.
 */

public class RunsAdapter extends RecyclerView.Adapter<RunsAdapter.ViewHolder> {

    private Context context;
    private List<Run> runList;
    private List<String> runKeys;
    private String uId;
    private int lastPosition = -1;
    private DatabaseReference runsRef;

    public RunsAdapter(Context context, String uId) {
        this.context = context;
        this.uId = uId;

        runList = new ArrayList<Run>();
        runKeys = new ArrayList<String>();

        runsRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_run, parent, false);

        return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        Run run = runList.get(position);
        final ViewHolder holder = h;

        holder.tvAuthor.setText(run.getAuthor());
        holder.tvTitle.setText(run.getTitle());
        holder.tvBody.setText(run.getBody());

        if (run.getImgUrl() != null) {
            Glide.with(context).load(run.getImgUrl()).into(holder.ivRunImage);
            holder.ivRunImage.setVisibility(View.VISIBLE);
        } else {
            holder.ivRunImage.setVisibility(View.GONE);
        }

        if (uId.equals(run.getUid())) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnDelete.setVisibility(View.INVISIBLE);
        }

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeRun(holder.getAdapterPosition());
            }
        });
    }

    public void removeRun(int index) {
        runsRef.child("runs").child(runKeys.get(index)).removeValue();

        runList.remove(index);
        runKeys.remove(index);
        notifyItemRemoved(index);
    }

    public void removeRunByKey(String key) {
        int index = runKeys.indexOf(key);
        if (index != -1) {
            runList.remove(index);
            runKeys.remove(index);
            notifyItemRemoved(index);
        }
    }

    @Override
    public int getItemCount() {
        return runList.size();
    }

    public void addRun(Run run, String key) {
        runList.add(run);
        runKeys.add(key);
        notifyDataSetChanged();
    }

    public void removeRun(Run run, String key) {
        runList.remove(run);
        runKeys.remove(key);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvAuthor;
        public TextView tvTitle;
        public TextView tvBody;
        public Button btnDelete;
        public ImageView ivRunImage;


        public ViewHolder(View itemView) {
            super(itemView);

            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvBody = itemView.findViewById(R.id.tvBody);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            ivRunImage = itemView.findViewById(R.id.ivRunImage);

        }
    }
}
