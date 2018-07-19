package fr.onecraft.chestevent.core.objects;

import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class Pager {
    private String event;
    private int size;
    private int currentPage;
    private List<TextComponent> messages;

    public Pager(String event, List<TextComponent> messages) {
        this.event = event;
        this.size = 15;
        this.currentPage = 1;
        this.messages = messages;
    }

    public List<TextComponent> getPage(int page) {
        currentPage = page;
        return new ArrayList<>(messages.subList((page - 1) * size, getPages() == page ? messages.size() : page * size));
    }

    public String getEvent() {
        return event;
    }

    public int getPages() {
        return (int) Math.ceil((double) messages.size() / size);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}