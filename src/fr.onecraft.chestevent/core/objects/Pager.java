package fr.onecraft.chestevent.core.objects;

import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;

public class Pager {

    public static final int PAGE_SIZE = 9;

    private final String event;
    private final List<TextComponent> messages;

    private int currentPage;

    public Pager(String event, List<TextComponent> messages) {
        this.event = event;
        this.currentPage = 1;
        this.messages = messages;
    }

    public List<TextComponent> getView() {
        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(messages.size(), currentPage * PAGE_SIZE);
        return messages.subList(from, to);
    }

    public String getEvent() {
        return event;
    }

    public int getMaxPage() {
        return (int) Math.ceil((double) messages.size() / PAGE_SIZE);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
