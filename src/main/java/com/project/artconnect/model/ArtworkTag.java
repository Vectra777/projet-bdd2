package com.project.artconnect.model;

public class ArtworkTag {
    private String name;

    public ArtworkTag() {
    }

    public ArtworkTag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
