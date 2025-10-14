package com.github.codeboy.api;

import com.github.codeboy.OpenMensa;
import com.github.codeboy.Util;
import com.github.codeboy.cache.MensaCacheManager;
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

    // Mapping of allergen codes to their full descriptions
    private static final Map<String, String> ALLERGEN_MAP = new HashMap<>();
    
    static {
        // Additives (numbered)
        ALLERGEN_MAP.put("1", "Farbstoff");
        ALLERGEN_MAP.put("2", "Konservierungsstoff");
        ALLERGEN_MAP.put("3", "Antioxidationsmittel");
        ALLERGEN_MAP.put("4", "Geschmacksverstärker");
        ALLERGEN_MAP.put("5", "geschwefelt");
        ALLERGEN_MAP.put("6", "geschwärzt");
        ALLERGEN_MAP.put("7", "gewachst");
        ALLERGEN_MAP.put("8", "Phosphat");
        ALLERGEN_MAP.put("9", "Süßungsmittel");
        ALLERGEN_MAP.put("10", "enthält eine Phenylalaninquelle");
        
        // Main allergen categories (letters)
        ALLERGEN_MAP.put("A", "Gluten");
        ALLERGEN_MAP.put("A1", "Weizen");
        ALLERGEN_MAP.put("A2", "Roggen");
        ALLERGEN_MAP.put("A3", "Gerste");
        ALLERGEN_MAP.put("A4", "Hafer");
        ALLERGEN_MAP.put("A5", "Dinkel");
        ALLERGEN_MAP.put("B", "Sellerie");
        ALLERGEN_MAP.put("C", "Krebstiere");
        ALLERGEN_MAP.put("D", "Eier");
        ALLERGEN_MAP.put("E", "Fische");
        ALLERGEN_MAP.put("F", "Erdnüsse");
        ALLERGEN_MAP.put("G", "Sojabohnen");
        ALLERGEN_MAP.put("H", "Milch");
        ALLERGEN_MAP.put("I", "Schalenfrüchte");
        ALLERGEN_MAP.put("I1", "Mandeln");
        ALLERGEN_MAP.put("I2", "Haselnüsse");
        ALLERGEN_MAP.put("I3", "Walnüsse");
        ALLERGEN_MAP.put("I4", "Kaschunüsse");
        ALLERGEN_MAP.put("I5", "Pecannüsse");
        ALLERGEN_MAP.put("I6", "Paranüsse");
        ALLERGEN_MAP.put("I7", "Pistazien");
        ALLERGEN_MAP.put("I8", "Macadamianüsse");
        ALLERGEN_MAP.put("J", "Senf");
        ALLERGEN_MAP.put("K", "Sesamsamen");
        ALLERGEN_MAP.put("L", "Schwefeldioxid oder Sulfite");
        ALLERGEN_MAP.put("M", "Lupinen");
        ALLERGEN_MAP.put("N", "Weichtiere");
    }

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
        List<Meal>meals=OpenMensa.getInstance().getMensa(187).getMeals(true);
        for(Meal meal: meals){
            System.out.println("\n"+meal.getName()+"\n");
            for(String s:meal.getNotes()){
                System.out.println("\""+s+"\"");
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

    public RWTHMensa(Mensa original, String webName, String otherWebname, int id) {
        this.original = original;
        this.webName = webName;
        this.otherWebname = otherWebname;
        this.id = id;
    }

    private MensaCacheManager getCacheManager(){
        return OpenMensa.getInstance().getCacheManager();
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
            //loadOpeningHours();
            loadMeals();
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadMeals() throws IOException, ParseException {
        String url = "https://www.studierendenwerk-aachen.de/speiseplaene/" + webName + "-w.html";
        Document doc = Jsoup.connect(url).get();

        String[] dateStrings = parseDates(doc);
        
        Elements dayPanels = doc.select("div.default-panel, div.active-panel");
        
        for (int i = 0; i < dayPanels.size(); i++) {
            Element dayPanel = dayPanels.get(i);
            List<Meal> mealsForDay = new ArrayList<>();
            
            parseMainMeals(dayPanel, mealsForDay);
            
            parseSideDishes(dayPanel, mealsForDay);
            
            getCacheManager().cacheMeals(id, dateStrings[i], mealsForDay);
        }
    }
    
    /**
     * Extracts and formats the dates from the menu page.
     */
    private String[] parseDates(Document doc) throws ParseException {
        Elements dateHeaders = doc.select("h3.default-headline, h3.active-headline");
        String[] dateStrings = new String[dateHeaders.size()];
        SimpleDateFormat parser = new SimpleDateFormat("dd.MM.yyyy");
        
        for (int i = 0; i < dateHeaders.size(); i++) {
            Element dateHeader = dateHeaders.get(i);
            String dateText = dateHeader.child(0).text();
            // Extract date from format "Montag, 13.10.2025"
            String dateString = dateText.split(" ")[1];
            Date dateObject = parser.parse(dateString);
            dateStrings[i] = Util.dateToString(dateObject);
        }
        
        return dateStrings;
    }
    
    /**
     * Parses main meals from a day panel.
     */
    private void parseMainMeals(Element dayPanel, List<Meal> mealsForDay) {
        Element menuesTable = dayPanel.selectFirst("table.menues");
        if (menuesTable == null) {
            return;
        }
        
        Element tbody = menuesTable.selectFirst("tbody");
        if (tbody == null) {
            return;
        }
        
        Elements mealRows = tbody.select("tr");
        for (Element mealRow : mealRows) {
            Meal meal = parseMealRow(mealRow);
            if (meal != null) {
                mealsForDay.add(meal);
            }
        }
    }
    
    /**
     * Parses a single meal row from the main meals table.
     */
    private Meal parseMealRow(Element mealRow) {
        // Extract dietary tags from CSS classes (e.g., vegan, OLV, Schwein, etc.)
        Set<String> dietaryTags = extractDietaryTags(mealRow);
        
        Element menueWrapper = mealRow.selectFirst("td.menue-wrapper");
        if (menueWrapper == null) {
            return null;
        }
        
        // Extract category (e.g., "Tellergericht", "Vegetarisch", etc.)
        Element categoryElement = menueWrapper.selectFirst("span.menue-category");
        String category = categoryElement != null ? categoryElement.text() : "";
        
        // Extract description and allergen information
        Element menueDesc = menueWrapper.selectFirst("span.menue-desc");
        if (menueDesc == null) {
            return null;
        }
        
        Element expandNutr = menueDesc.selectFirst("span.expand-nutr");
        if (expandNutr == null) {
            return null;
        }
        
        // Get the meal description (text before allergen info)
        String description = expandNutr.ownText();
        
        // Extract allergen information from <sup> tags
        List<String> allergens = extractAllergens(expandNutr);
        
        // Combine dietary tags with allergen information
        List<String> notes = new ArrayList<>(dietaryTags);
        notes.addAll(allergens);
        
        // Extract price
        String price = extractPrice(menueWrapper);
        
        return new Meal(description, category, notes, new Prices(price, null, null, null));
    }
    
    /**
     * Extracts dietary tags from CSS classes, filtering out non-dietary classes.
     */
    private Set<String> extractDietaryTags(Element mealRow) {
        Set<String> tags = new HashSet<>(mealRow.classNames());
        // Remove CSS styling classes that are not dietary information
        tags.remove("bg-color");
        tags.remove("even");
        tags.remove("odd");
        return tags;
    }
    
    /**
     * Extracts allergen information from <sup> tags and converts codes to full descriptions.
     * Example: <sup> A,A1,A3,A5</sup> -> ["Gluten", "Weizen", "Gerste", "Dinkel"]
     */
    private List<String> extractAllergens(Element expandNutr) {
        List<String> allergens = new ArrayList<>();
        Elements supElements = expandNutr.select("sup");
        
        for (Element sup : supElements) {
            String allergenText = sup.text().trim();
            if (!allergenText.isEmpty() && !allergenText.equals("Preis ohne Pfand")) {
                // Split by comma and add each allergen code
                String[] codes = allergenText.split(",");
                for (String code : codes) {
                    String trimmedCode = code.trim();
                    if (!trimmedCode.isEmpty()) {
                        // Convert code to full description, fallback to code if not found
                        String allergenName = ALLERGEN_MAP.getOrDefault(trimmedCode, trimmedCode);
                        System.out.println(trimmedCode+" "+allergenName);
                        allergens.add(allergenName);
                    }
                }
            }
        }
        
        return allergens;
    }
    
    /**
     * Extracts price from the menu wrapper element.
     */
    private String extractPrice(Element menueWrapper) {
        Element priceElement = menueWrapper.selectFirst("span.menue-price");
        if (priceElement == null) {
            return "0.0";
        }
        
        String priceText = priceElement.text().trim();
        // Extract price before the € symbol and replace comma with dot
        String[] parts = priceText.split(" ");
        if (parts.length > 0) {
            return parts[0].replace(",", ".");
        }
        
        return "0.0";
    }
    
    /**
     * Parses side dishes (Beilagen) from a day panel.
     */
    private void parseSideDishes(Element dayPanel, List<Meal> mealsForDay) {
        Element extrasTable = dayPanel.selectFirst("table.extras");
        if (extrasTable == null) {
            return;
        }
        
        Elements beilagenWrappers = extrasTable.select("td.menue-wrapper");
        Prices noPrices = new Prices(null, null, null, null);
        
        for (Element wrapper : beilagenWrappers) {
            Element categoryElement = wrapper.selectFirst("span.menue-category");
            String category = categoryElement != null ? categoryElement.text() : "Beilage";
            
            Element menueDesc = wrapper.selectFirst("span.menue-desc");
            if (menueDesc == null) {
                continue;
            }
            
            // Parse each side dish (text nodes separated by "oder")
            for (TextNode textNode : menueDesc.textNodes()) {
                String text = textNode.text().trim();
                // Remove the leading "+" if present
                text = text.replaceFirst("^\\+\\s*", "").trim();
                
                if (!text.isEmpty() && !text.equals("oder")) {
                    // Extract allergens from sup tags
                    List<String> allergens = extractAllergens(menueDesc);
                    
                    Meal sideDish = new Meal(text, category, allergens, noPrices);
                    mealsForDay.add(sideDish);
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
    public List<Meal> getMeals(Date date, boolean bypassCache) {
        return getMeals(Util.dateToString(date), bypassCache);
    }

    @Override
    public List<Meal> getMeals(String date) {
        return getMeals(date, false);
    }

    @Override
    public List<Meal> getMeals(String date, boolean bypassCache) {
        if (!bypassCache) {
            List<Meal> cachedMeals = getCacheManager().getCachedMeals(id, date);
            if (cachedMeals != null) {
                return cachedMeals;
            }
        }
        
        loadNewMeals();
        
        List<Meal> freshMeals = getCacheManager().getCachedMeals(id, date);
        return freshMeals != null ? freshMeals : Collections.emptyList();
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
