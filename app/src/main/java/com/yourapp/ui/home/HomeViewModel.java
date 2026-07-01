package com.yourapp.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.model.Conversation;
import java.util.List;

/**
 * ViewModel for HomeActivity.
 * Exposes three LiveData streams:
 *   - conversationList: the loaded list
 *   - loading: true while network call is in-flight
 *   - error: non-null on failure ("UNAUTHORIZED" triggers redirect to Login)
 */
public class HomeViewModel extends ViewModel {

    private final ConversationRepository conversationRepository;

    private final MutableLiveData<List<Conversation>> conversationList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public HomeViewModel(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public LiveData<List<Conversation>> getConversationList() {
        return conversationList;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    /**
     * Loads conversations from the repository and posts results to LiveData.
     * Observes the repository LiveData internally and re-posts values so that
     * the Activity lifecycle stays clean.
     */
    public void loadConversations() {
        loading.setValue(true);

        conversationRepository.getConversations().observeForever(result -> {
            loading.postValue(false);

            if (result instanceof AuthRepository.Result.Success) {
                AuthRepository.Result.Success<List<Conversation>> success =
                        (AuthRepository.Result.Success<List<Conversation>>) result;
                conversationList.postValue(success.data);
            } else if (result instanceof AuthRepository.Result.Error) {
                AuthRepository.Result.Error<List<Conversation>> err =
                        (AuthRepository.Result.Error<List<Conversation>>) result;
                error.postValue(err.message);
            }
        });
    }
}
