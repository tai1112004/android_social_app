package com.yourapp.model;

public class ReactionRequest {
    private String reaction;

    public ReactionRequest(String reaction) {
        this.reaction = reaction;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }
}
