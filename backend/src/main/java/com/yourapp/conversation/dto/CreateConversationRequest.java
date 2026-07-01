package com.yourapp.conversation.dto;

import java.util.List;

public class CreateConversationRequest {

    private String type;
    private String name;
    private List<Long> memberIds;

    public CreateConversationRequest() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }
}
