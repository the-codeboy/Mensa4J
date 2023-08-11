package com.github.codeboy.api;

import java.util.Date;
import java.util.List;

public interface Mensa {
    default List<Meal> getMeals() {
        return getMeals(new Date());
    }

    List<Meal> getMeals(Date date);

    List<Meal> getMeals(String date);

    boolean isOpen();

    boolean isOpen(Date date);

    boolean isOpen(String date);

    int getId();

    String getName();

    String getCity();

    String getAddress();

    List<Double> getCoordinates();
}
