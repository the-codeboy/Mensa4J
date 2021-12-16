package com.github.codeboy.api;

import java.util.List;

public class Meal {
    private String name, category;
    private List<String> notes;
    private Prices prices;

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getNotes() {
        return notes;
    }

    public Prices getPrices() {
        return prices;
    }
}
