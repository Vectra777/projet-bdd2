package com.project.artconnect.model;

import java.util.ArrayList;
import java.util.List;

public class Gallery {
    private String name;
    private String address;
    private String ownerName;
    private String openingHours;
    private String contactPhone;
    private double rating;
    private String website;
    private List<Exhibition> exhibitions = new ArrayList<>();

    public Gallery() {
    }

    public Gallery(String name, String address, double rating) {
        this.name = name;
        this.address = address;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public List<Exhibition> getExhibitions() {
        return exhibitions;
    }

    public void setExhibitions(List<Exhibition> exhibitions) {
        this.exhibitions = exhibitions;
    }

    public void addExhibition(Exhibition exhibition) {
        this.exhibitions.add(exhibition);
        if (exhibition.getGallery() != this) {
            exhibition.setGallery(this);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
