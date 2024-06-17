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


    /**
     * @return true if this object provides the opening hours of the mensa
     */
    boolean hasOpeningHours();

    /**
     * @return the hour of day where the mensa opens - or 0 if unknown
     */
    default float getOpeningTime(Date date) {
        return 0;
    }

    /**
     * @return the hour of day where the mensa closes - or 0 if unknown
     */
    default float getClosingTime(Date date) {
        return 0;
    }
}
