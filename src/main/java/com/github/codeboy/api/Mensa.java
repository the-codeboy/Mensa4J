package com.github.codeboy.api;

import java.util.Date;
import java.util.List;

public interface Mensa {
    List<Meal> getMeals();

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
