package com.example.platforme_backend;

import java.util.List;

public class PagedPlayersResponse {
    private List<PlayerProfileDTO> players;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public PagedPlayersResponse() {}

    public PagedPlayersResponse(List<PlayerProfileDTO> players, long totalElements,
                                int totalPages, int currentPage, int pageSize) {
        this.players = players;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    // Getters et Setters
    public List<PlayerProfileDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerProfileDTO> players) { this.players = players; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}