package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;

public class ResourceRating implements Serializable {
    @Serial
    private static final long serialVersionUID = 6067902935147428053L;

    private final String type;
    private final HashMap<Integer, Integer> userRates = new HashMap<>();

    private transient float average = Float.NaN;

    public ResourceRating(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean isRated(int userId) {
        return userRates.containsKey(userId);
    }

    public Integer getRate(int userId) {
        return userRates.getOrDefault(userId, null);
    }

    public void addRate(int userId, int value) {
        userRates.put(userId, value);
    }

    public int countRates(int value) {
        return (int) userRates.values().stream().filter(v -> v == value).count();
    }

    public int total() {
        return userRates.size();
    }

    public float average() {
        if (Float.isNaN(average) && !userRates.isEmpty()) {
            int sum = 0;
            for (Integer rating : userRates.values()) {
                sum += rating;
            }
            this.average = (float) sum / userRates.size();
        }
        return average;
    }
}
