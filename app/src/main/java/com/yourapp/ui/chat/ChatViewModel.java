package com.yourapp.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.MessageRepository;
import com.yourapp.model.Message;
import com.yourapp.model.SendMessageRequest;
import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final MessageRepository messageRepository;
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ChatViewModel(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSending() {
        return sending;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadMessages(Long conversationId) {
        loading.setValue(true);
        messageRepository.getMessages(conversationId).observeForever(result -> {
            loading.postValue(false);
            if (result instanceof AuthRepository.Result.Success) {
                AuthRepository.Result.Success<List<Message>> success =
                        (AuthRepository.Result.Success<List<Message>>) result;
                messages.postValue(success.data);
            } else if (result instanceof AuthRepository.Result.Error) {
                AuthRepository.Result.Error<List<Message>> err =
                        (AuthRepository.Result.Error<List<Message>>) result;
                error.postValue(err.message);
            }
        });
    }

    public void replaceMessages(List<Message> newMessages) {
        messages.postValue(newMessages != null ? new ArrayList<>(newMessages) : new ArrayList<>());
    }

    public void appendIncomingMessage(Message message) {
        if (message == null) {
            return;
        }
        List<Message> current = messages.getValue();
        List<Message> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
        if (message.getId() != null) {
            for (Message existing : updated) {
                if (existing.getId() != null && existing.getId().equals(message.getId())) {
                    return;
                }
            }
        }
        updated.add(message);
        messages.postValue(updated);
    }

    public void sendMessage(Long conversationId, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        SendMessageRequest request = new SendMessageRequest(content.trim(), "TEXT");
        sendMessage(conversationId, request);
    }

    public void sendMessage(Long conversationId, SendMessageRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            return;
        }

        sending.setValue(true);
        messageRepository.sendMessage(conversationId, request).observeForever(result -> {
            sending.postValue(false);
            if (result instanceof AuthRepository.Result.Success) {
                AuthRepository.Result.Success<Message> success =
                        (AuthRepository.Result.Success<Message>) result;
                appendIncomingMessage(success.data);
            } else if (result instanceof AuthRepository.Result.Error) {
                AuthRepository.Result.Error<Message> err =
                        (AuthRepository.Result.Error<Message>) result;
                error.postValue(err.message);
            }
        });
    }

    public void deleteMessage(Message original) {
        if (original == null || original.getId() == null) {
            return;
        }
        messageRepository.deleteMessage(original.getId()).observeForever(result -> {
            if (result instanceof AuthRepository.Result.Success) {
                List<Message> current = messages.getValue();
                if (current == null) {
                    return;
                }
                List<Message> updated = new ArrayList<>(current);
                updated.removeIf(item -> item.getId() != null && item.getId().equals(original.getId()));
                messages.postValue(updated);
            } else if (result instanceof AuthRepository.Result.Error) {
                error.postValue(((AuthRepository.Result.Error<Object>) result).message);
            }
        });
    }
    public void reactToMessage(Message original, String reaction) {
        if (original == null || original.getId() == null || reaction == null || reaction.isEmpty()) {
            return;
        }
        messageRepository.reactToMessage(original.getId(), reaction).observeForever(result -> {
            if (result instanceof AuthRepository.Result.Success) {
                Message updatedMessage = ((AuthRepository.Result.Success<Message>) result).data;
                List<Message> current = messages.getValue();
                if (current == null) {
                    return;
                }
                List<Message> updated = new ArrayList<>(current);
                for (int i = 0; i < updated.size(); i++) {
                    Message item = updated.get(i);
                    if (item.getId() != null && item.getId().equals(updatedMessage.getId())) {
                        updated.set(i, updatedMessage);
                        messages.postValue(updated);
                        return;
                    }
                }
            } else if (result instanceof AuthRepository.Result.Error) {
                error.postValue(((AuthRepository.Result.Error<Message>) result).message);
            }
        });
    }
}

