package com.github.codeboy;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private static final Gson gson = new Gson();

    public static String dateToString(Date date) {
        return format.format(date);
    }

    public static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public static <T> T getObject(String url, Type type) throws Exception {
        return gson.fromJson(Util.readUrl(url), type);
    }
}
