package com.yourapp.conversation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationInfoDto {

    private Long id;
    private String type;
    private String name;
    private int memberCount;
    private LocalDateTime createdAt;
    private List<ConversationMemberDto> members;

    public ConversationInfoDto() {
    }

    public ConversationInfoDto(Long id, String type, String name, int memberCount,
                               LocalDateTime createdAt, List<ConversationMemberDto> members) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.members = members;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ConversationMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<ConversationMemberDto> members) {
        this.members = members;
    }
}
