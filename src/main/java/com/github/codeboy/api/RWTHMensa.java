package com.github.codeboy.api;

import com.github.codeboy.OpenMensa;
import com.github.codeboy.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.*;

public class RWTHMensa implements Mensa {

    public static void injectRWTHCanteens(HashMap<Integer, Mensa> canteens) {
        injectCanteen(canteens, 187, "academica");
        injectCanteen(canteens, 96, "vita");
        injectCanteen(canteens, 97, "bayernallee");
        injectCanteen(canteens, 95, "ahornstrasse");
        injectCanteen(canteens, 94, "templergraben", "bistro-templergraben");// yes for some reason this has multiple names
        injectCanteen(canteens, 98, "eupenerstrasse", "eupener-strasse");
        injectCanteen(canteens, 99, "goethestrasse");
        //injectCanteen(canteens, 93, "forum","suedpark");// this getting to complicated. Why do they have a completely different route just for this one???
        injectCanteen(canteens, 100, "juelich");
    }

    public static void main(String[] args) {
        OpenMensa.getInstance().reloadCanteens();
        for (Mensa mensa : OpenMensa.getInstance().getAllCanteens()) {
            if (mensa instanceof RWTHMensa) {
                System.out.println(mensa.getId());
                System.out.println(mensa.getName());
            }
        }
    }

    private static void injectCanteen(HashMap<Integer, Mensa> canteens, int id, String webName) {
        injectCanteen(canteens, id, webName, webName);
    }

    private static void injectCanteen(HashMap<Integer, Mensa> canteens, int id, String webName, String otherWebName) {
        Mensa original = canteens.get(id);
        RWTHMensa rwthMensa = new RWTHMensa(original, webName, otherWebName, id);
        canteens.put(id, rwthMensa);// override the original mensa
    }

    private final Mensa original;
    private final String webName;
    private final String otherWebname;
    private final int id;
    private final Map<String, OpeningTimes> openingTimesMap = new HashMap<>();

    private HashMap<String, List<Meal>> meals = new HashMap<>();

    public RWTHMensa(Mensa original, String webName, String otherWebname, int id) {
        this.original = original;
        this.webName = webName;
        this.otherWebname = otherWebname;
        this.id = id;
        try {
            loadOpeningHours();
            loadMeals();
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadOpeningHours() throws IOException {
        String url = "https://www.studierendenwerk-aachen.de/de/Gastronomie/mensa-" + otherWebname + "-wochenplan.html";
        Document doc = Jsoup.connect(url).get();
        Elements divs = doc.select("div.openings");

        for (Element div : divs) {
            String openingTimesText = div.text().trim();
            if (!openingTimesText.isEmpty() && !openingTimesText.equals(" ")) {
                // Pattern to match day ranges and time ranges
                Pattern pattern = Pattern.compile("(Mo\\.|Di\\.|Mi\\.|Do\\.|Fr\\.|Sa\\.|So\\.)\\.?\\s*(?:−\\s*(Mo\\.|Di\\.|Mi\\.|Do\\.|Fr\\.|Sa\\.|So\\.)\\.?)?\\s*\\d{2}:\\d{2}−\\d{2}:\\d{2}");
                Matcher matcher = pattern.matcher(openingTimesText);

                while (matcher.find()) {
                    String match = matcher.group();
                    String startDay, endDay, times;
                    if (match.split("−").length == 2) {// check if this is one date or a date range
                        String[] parts = match.split("\\.");
                        startDay = parts[0];
                        endDay = startDay;
                        times = parts[1].replace(" ", "");
                    } else {
                        String[] parts = match.split(" ");
                        String[] days = parts[0].split("−");
                        startDay = days[0].replace(".", "");
                        endDay = days[1].replace(".", "");
                        times = parts[1];
                    }

                    // Parse times into floats
                    String[] timeParts = times.split("−");
                    float startTime = parseTime(timeParts[0]);
                    float endTime = parseTime(timeParts[1]);

                    // Populate the map for all days in the range
                    for (String day = startDay; !day.equals(endDay); day = getNextDay(day)) {
                        openingTimesMap.put(day, new OpeningTimes(startTime, endTime));
                    }
                    openingTimesMap.put(endDay, new OpeningTimes(startTime, endTime)); // Add the end day as well
                }
                break;
            }
        }
    }

    // Helper function to parse time string into float (e.g., "11:30" -> 11.5)
    private static float parseTime(String timeString) {
        String[] parts = timeString.split(":");
        return Integer.parseInt(parts[0]) + (Float.parseFloat(parts[1]) / 60);
    }

    // Helper function to get the next day in the week (e.g., "Mo" -> "Di")
    private static String getNextDay(String day) {
        switch (day) {
            case "Mo":
                return "Di";
            case "Di":
                return "Mi";
            case "Mi":
                return "Do";
            case "Do":
                return "Fr";
            case "Fr":
                return "Sa";
            case "Sa":
                return "So";
            case "So":
                return "Mo";
            default:
                throw new IllegalArgumentException(day);
        }
    }


    public void loadNewMeals() {
        try {
            loadOpeningHours();// probably not needed but i do not know how often they change them
            loadMeals();
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadMeals() throws IOException, ParseException {// todo refractor this method and maybe move to different class
        String url = "https://www.studierendenwerk-aachen.de/speiseplaene/" + webName + "-w.html";
        Document doc = Jsoup.connect(url).get();
        Elements days = doc.select("div.default-panel, div.active-panel");
        Elements dates = doc.select("h3.default-headline, h3.active-headline");
        String[] dateStrings = new String[dates.size()];
        SimpleDateFormat parser = new SimpleDateFormat("dd.MM.yyyy");
        for (int i = 0, datesSize = dates.size(); i < datesSize; i++) {
            Element date = dates.get(i);
            String dateString = date.child(0).text();
            dateString = dateString.split(" ")[1];
            Date dateObject = parser.parse(dateString);
            dateStrings[i] = Util.dateToString(dateObject);
        }
        for (int i = 0; i < days.size(); i++) {
            Element day = days.get(i);
            ArrayList<Meal> meals = new ArrayList<>();
            this.meals.put(dateStrings[i], meals);

            Element menues = day.selectFirst("table.menues").selectFirst("tbody");
            Elements mealHTMLs = menues.select("tr");
            for (Element mealHTML : mealHTMLs) {
                Set<String> tags = mealHTML.classNames();
                tags.remove("bg-color");
                tags.remove("even");
                tags.remove("odd");// css classes which are always there

                Element menueWrapper = mealHTML.selectFirst("td.menue-wrapper");
                String category = menueWrapper.selectFirst("span.menue-category").text();
                Element mealDescription = menueWrapper.selectFirst("span.menue-desc")
                        .selectFirst("span.expand-nutr");

                String description = mealDescription.ownText();

                Elements descriptionParts = mealDescription.children();
                // todo extract the allergy stuff from this
                Element mealPrice = menueWrapper.selectFirst("span.menue-price");
                String price;
                if (mealPrice == null)
                    price = "0.0";
                else price = mealPrice.text().split(" ")[0].replace(",", ".");

                Meal meal = new Meal(description, category, new ArrayList<>(tags),
                        new Prices(price, null, null, null));
                meals.add(meal);
            }

            Element extras = day.selectFirst("table.extras");// parse beilagen
            Elements beilagen = extras.select("td.menue-wrapper");
            Prices prices = new Prices(null, null, null, null);
            for (Element beilage : beilagen) {
                String category = beilage.selectFirst("span.menue-category").text();
                for (TextNode beilagenNode : beilage.selectFirst("span.menue-desc").textNodes()) {
                    String description = beilagenNode.text();
                    Meal meal = new Meal(description, category, Collections.emptyList(), prices);
                    meals.add(meal);
                }
            }
        }
    }

    public void reloadMeals() throws IOException, ParseException {
        loadMeals();
    }

    private OpeningTimes getOpeningTimes(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        String day = getDayName(dayOfWeek);
        OpeningTimes openingTimes = openingTimesMap.get(day);
        if (openingTimes == null)
            return OpeningTimes.closed;
        return openingTimes;
    }

    String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case SUNDAY:
                return "So";
            case MONDAY:
                return "Mo";
            case TUESDAY:
                return "Di";
            case WEDNESDAY:
                return "Mi";
            case THURSDAY:
                return "Do";
            case FRIDAY:
                return "Fr";
            case SATURDAY:
                return "Sa";
            default:
                throw new IllegalStateException("Unexpected value: " + dayOfWeek);
        }
    }

    @Override
    public List<Meal> getMeals(Date date) {
        return getMeals(Util.dateToString(date));
    }

    @Override
    public List<Meal> getMeals(String date) {
        if (!meals.containsKey(date))
            loadNewMeals();
        return meals.getOrDefault(date, Collections.emptyList());
    }

    @Override
    public boolean isOpen() {
        return isOpen(new Date());
    }

    @Override
    public boolean isOpen(Date date) {
        return isOpen(Util.dateToString(date));
    }

    @Override
    public boolean isOpen(String date) {
        return !getMeals(date).isEmpty();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        if (original == null) {
            return webName;
        }
        return original.getName();
    }

    @Override
    public String getCity() {
        if (original == null) {
            return "Aachen";
        }
        return original.getCity();
    }

    @Override
    public String getAddress() {
        if (original == null) {
            return "unknown";
        }
        return original.getAddress();
    }

    @Override
    public List<Double> getCoordinates() {
        if (original == null) {
            Double[] d = {0.0,0.0};
            return Arrays.asList(d);
        }
        return original.getCoordinates();
    }

    @Override
    public boolean hasOpeningHours() {
        return true;
    }

    @Override
    public float getOpeningTime(Date date) {
        return getOpeningTimes(date).startTime;
    }


    @Override
    public float getClosingTime(Date date) {
        return getOpeningTimes(date).endTime;
    }
}
