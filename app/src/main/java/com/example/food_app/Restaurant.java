package com.example.food_app;

public class Restaurant {
    private String name;
    private int priceForTwo;
    private String url;
    private String cuisines;
    private double latitude;
    private double longitude;
    Restaurant(String theName, int thePriceForTwo, String theUrl, String theCuisines, double theLat, double theLong) {
        name = theName;
        priceForTwo = thePriceForTwo;
        url = theUrl;
        cuisines = theCuisines;
        latitude = theLat;
        longitude = theLong;
    }
    public String getName() {
        return name;
    }
    public void setName(String newName) {
        name = newName;
    }
    public int getPrice() {
        return priceForTwo;
    }
    public void setPrice(int newPrice) {
        priceForTwo = newPrice;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String newUrl) {
        url = newUrl;
    }
    public String getCuisines() {
        return cuisines;
    }
    public void setCuisines(String newCuisines) {
        cuisines = newCuisines;
    }
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double newLat) {
        latitude = newLat;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double newLong) {
        longitude = newLong;
    }
}
