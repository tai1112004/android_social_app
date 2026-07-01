package com.yourapp.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.model.Conversation;
import java.util.List;

public class HomeViewModel extends ViewModel {
   private final ConversationRepository conversationRepository;
   private final MutableLiveData<List<Conversation>> conversationList = new MutableLiveData();
   private final MutableLiveData<Boolean> loading = new MutableLiveData(false);
   private final MutableLiveData<String> error = new MutableLiveData();

   public HomeViewModel(ConversationRepository conversationRepository) {
      this.conversationRepository = conversationRepository;
   }

   public LiveData<List<Conversation>> getConversationList() {
      return this.conversationList;
   }

   public LiveData<Boolean> getLoading() {
      return this.loading;
   }

   public LiveData<String> getError() {
      return this.error;
   }

   public void loadConversations() {
      this.loading.setValue(true);
      this.conversationRepository.getConversations().observeForever((result) -> {
         this.loading.postValue(false);
         if (result instanceof AuthRepository.Result.Success<List<Conversation>> success) {
            this.conversationList.postValue((List)success.data);
         } else if (result instanceof AuthRepository.Result.Error<List<Conversation>> err) {
            this.error.postValue(err.message);
         }

      });
   }
}
