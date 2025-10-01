package com.github.codeboy.api;

import java.util.Date;
import java.util.List;

public interface Mensa {
    default List<Meal> getMeals() {
        return getMeals(new Date());
    }

    default List<Meal> getMeals(boolean bypassCache) {
        return getMeals(new Date(), bypassCache);
    }

    List<Meal> getMeals(Date date);

    default List<Meal> getMeals(Date date, boolean bypassCache) {
        return getMeals(date);
    }

    List<Meal> getMeals(String date);

    default List<Meal> getMeals(String date, boolean bypassCache) {
        return getMeals(date);
    }

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
