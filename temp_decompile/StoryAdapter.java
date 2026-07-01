package com.yourapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R.drawable;
import com.yourapp.R.id;
import com.yourapp.R.layout;
import com.yourapp.model.Story;
import com.yourapp.model.User;
import com.yourapp.util.AvatarUtil;
import java.util.ArrayList;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryViewHolder> {
   private static final int TYPE_ADD_STORY = 0;
   private static final int TYPE_STORY = 1;
   private List<Story> stories = new ArrayList();
   private User currentUser;
   private OnStoryActionListener listener;

   public StoryAdapter(OnStoryActionListener listener) {
      this.listener = listener;
   }

   public void setStories(List<Story> stories, User currentUser) {
      this.stories = stories;
      this.currentUser = currentUser;
      this.notifyDataSetChanged();
   }

   public int getItemViewType(int position) {
      return position == 0 ? 0 : 1;
   }

   @NonNull
   public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View v = LayoutInflater.from(parent.getContext()).inflate(layout.item_story, parent, false);
      return new StoryViewHolder(v);
   }

   public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
      if (this.getItemViewType(position) == 0) {
         holder.tvName.setText("Tin của tôi");
         holder.ivAvatar.setVisibility(8);
         holder.tvAvatar.setVisibility(0);
         holder.tvAvatar.setText("+");
         holder.tvAvatar.setTextColor(-14326805);
         holder.tvAvatar.setBackgroundResource(drawable.bg_empty_icon);
         holder.itemView.setOnClickListener((v) -> {
            if (this.listener != null) {
               this.listener.onAddStory();
            }

         });
      } else {
         Story story = (Story)this.stories.get(position - 1);
         String name = story.getAuthorDisplayName() != null && !story.getAuthorDisplayName().isEmpty() ? story.getAuthorDisplayName() : story.getAuthorUsername();
         holder.tvName.setText(name);
         holder.tvAvatar.setTextColor(-15656921);
         holder.tvAvatar.setBackgroundResource(drawable.bg_story_ring);
         AvatarUtil.loadAvatar(story.getAuthorAvatarUrl(), story.getAuthorDisplayName(), story.getAuthorUsername(), holder.ivAvatar, holder.tvAvatar);
         holder.itemView.setOnClickListener((v) -> {
            if (this.listener != null) {
               this.listener.onViewStory(story);
            }

         });
      }

   }

   public int getItemCount() {
      return this.stories.size() + 1;
   }

   static class StoryViewHolder extends RecyclerView.ViewHolder {
      ImageView ivAvatar;
      TextView tvAvatar;
      TextView tvName;

      StoryViewHolder(View v) {
         super(v);
         this.ivAvatar = (ImageView)v.findViewById(id.iv_story_avatar);
         this.tvAvatar = (TextView)v.findViewById(id.tv_story_avatar);
         this.tvName = (TextView)v.findViewById(id.tv_story_name);
      }
   }

   public interface OnStoryActionListener {
      void onAddStory();

      void onViewStory(Story story);
   }
}
