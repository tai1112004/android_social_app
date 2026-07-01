package com.yourapp.ui.call;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.data.remote.CallApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.CallHistory;
import com.yourapp.model.CallHistoryPage;
import com.yourapp.network.RetrofitClient;
import com.yourapp.util.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallHistoryFragment extends Fragment {

    private final List<CallHistory> items = new ArrayList<>();
    private CallHistoryAdapter adapter;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvEmpty = view.findViewById(R.id.tv_history_empty);
        RecyclerView rv = view.findViewById(R.id.rv_call_history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CallHistoryAdapter(items);
        rv.setAdapter(adapter);
        loadHistory();
    }

    private void loadHistory() {
        TokenManager tokenManager = new TokenManager(requireContext());
        if (!tokenManager.hasValidTokens()) {
            renderEmpty();
            return;
        }
        CallApiService apiService = RetrofitClient.getInstance(tokenManager).create(CallApiService.class);
        apiService.getCallHistory(tokenManager.getUserId(), 0, 50).enqueue(new Callback<ApiResponse<CallHistoryPage>>() {
            @Override
            public void onResponse(Call<ApiResponse<CallHistoryPage>> call, Response<ApiResponse<CallHistoryPage>> response) {
                if (!response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) {
                    renderEmpty();
                    return;
                }
                CallHistoryPage page = response.body().getData();
                items.clear();
                if (page != null && page.getItems() != null) {
                    items.addAll(page.getItems());
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<CallHistoryPage>> call, Throwable t) {
                renderEmpty();
            }
        });
    }

    private void renderEmpty() {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.VISIBLE);
        }
        if (adapter != null) {
            items.clear();
            adapter.notifyDataSetChanged();
        }
    }

    private static class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.VH> {
        private final List<CallHistory> items;

        CallHistoryAdapter(List<CallHistory> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_call_history, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CallHistory item = items.get(position);
            String title = item.getCallerName() != null ? item.getCallerName() : "Cuoc goi";
            holder.tvAvatar.setText(title.isEmpty() ? "C" : title.substring(0, 1).toUpperCase());
            holder.tvTitle.setText(title);
            String status = item.getStatus() != null ? item.getStatus() : "UNKNOWN";
            holder.tvSubtitle.setText(status + " | " + item.getDurationSeconds() + "s");
            holder.tvTime.setText(item.getStartTime() != null ? item.getStartTime().replace('T', ' ') : "");
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvAvatar;
            final TextView tvTitle;
            final TextView tvSubtitle;
            final TextView tvTime;

            VH(@NonNull View itemView) {
                super(itemView);
                tvAvatar = itemView.findViewById(R.id.tv_history_avatar);
                tvTitle = itemView.findViewById(R.id.tv_history_title);
                tvSubtitle = itemView.findViewById(R.id.tv_history_subtitle);
                tvTime = itemView.findViewById(R.id.tv_history_time);
            }
        }
    }
}
