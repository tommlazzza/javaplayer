package com.dreamteam.control;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class QueueManager {
    private final Queue<String> queue = new LinkedList<>();

    public void add(String song) {
        if (!queue.contains(song)) queue.add(song);
    }

    public void addToTop(String song) {
        if (!queue.contains(song)) {
            Queue<String> temp = new LinkedList<>();
            temp.add(song);
            temp.addAll(queue);
            queue.clear();
            queue.addAll(temp);
        }
    }
    
    public boolean contains(String song) {
        return queue.contains(song);
    }

    public String poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public List<String> asList() {
        return new LinkedList<>(queue);
    }

    public void replaceAll(List<String> newQueue) {
        queue.clear();
        queue.addAll(newQueue);
    }
    
    public Iterator<String> iterator() {
        return queue.iterator();
    }
}
