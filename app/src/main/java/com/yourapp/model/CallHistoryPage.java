package com.yourapp.model;

import java.util.List;

public class CallHistoryPage {
    private List<CallHistory> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public List<CallHistory> getItems() { return items; }
    public void setItems(List<CallHistory> items) { this.items = items; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
