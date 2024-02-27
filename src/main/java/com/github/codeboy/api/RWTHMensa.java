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

public class RWTHMensa implements Mensa {

    public static void injectRWTHCanteens(HashMap<Integer, Mensa> canteens) {
        injectCanteen(canteens, 187, "academica");
        injectCanteen(canteens, 96, "vita");
        injectCanteen(canteens, 97, "bayernallee");
        injectCanteen(canteens, 95, "ahornstrasse");
        injectCanteen(canteens, 94, "templergraben");
    }

    public static void main(String[] args) {
        OpenMensa.getInstance().reloadCanteens();
    }

    private static void injectCanteen(HashMap<Integer, Mensa> canteens, int id, String webName) {
        Mensa original = canteens.get(id);
        RWTHMensa rwthMensa = new RWTHMensa(original, webName);
        canteens.put(id, rwthMensa);// override the original mensa
    }

    private final Mensa original;
    private final String webName;

    private HashMap<String, List<Meal>> meals = new HashMap<>();

    public RWTHMensa(Mensa original, String webName) {
        this.original = original;
        this.webName = webName;
        try {
            loadMeals();
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadNewMeals(){
        try {
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
                if(mealPrice==null)
                    price="0.0";
                 else price=mealPrice.text().split(" ")[0].replace(",",".");

                Meal meal = new Meal(description, category, new ArrayList<>(tags),
                        new Prices(price, null, null, null));
                meals.add(meal);
            }

            Element extras = day.selectFirst("table.extras");// parse beilagen
            Elements beilagen=extras.select("td.menue-wrapper");
            Prices prices=new Prices(null,null,null,null);
            for (Element beilage:beilagen){
                String category=beilage.selectFirst("span.menue-category").text();
                for (TextNode beilagenNode:beilage.selectFirst("span.menue-desc").textNodes()){
                    String description=beilagenNode.text();
                    Meal meal=new Meal(description,category,Collections.emptyList(),prices);
                    meals.add(meal);
                }
            }
        }
    }

    @Override
    public List<Meal> getMeals(Date date) {
        return getMeals(Util.dateToString(date));
    }

    @Override
    public List<Meal> getMeals(String date) {
        if(!meals.containsKey(date))
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
        return original.getId();
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public String getCity() {
        return original.getCity();
    }

    @Override
    public String getAddress() {
        return original.getAddress();
    }

    @Override
    public List<Double> getCoordinates() {
        return original.getCoordinates();
    }
}
