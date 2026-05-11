package com.project.artconnect.service.impl;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.GalleryService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * InMemory implementation of ExhibitionService.
 * Exhibitions are stored inside galleries (consistent with InMemoryGalleryService).
 */
public class InMemoryExhibitionService implements ExhibitionService {
    private final GalleryService galleryService;
    private final Map<String, Exhibition> standaloneExhibitions = new LinkedHashMap<>();

    public InMemoryExhibitionService(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @Override
    public List<Exhibition> getAllExhibitions() {
        List<Exhibition> all = new ArrayList<>();
        for (Gallery g : galleryService.getAllGalleries()) {
            all.addAll(g.getExhibitions());
        }
        all.addAll(standaloneExhibitions.values());
        return all;
    }

    @Override
    public Optional<Exhibition> getExhibitionByTitle(String title) {
        return getAllExhibitions().stream()
                .filter(e -> e.getTitle() != null && e.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    @Override
    public void createExhibition(Exhibition exhibition) {
        if (exhibition == null || exhibition.getTitle() == null)
            return;
        if (exhibition.getGallery() != null) {
            exhibition.getGallery().addExhibition(exhibition);
        } else {
            standaloneExhibitions.put(exhibition.getTitle(), exhibition);
        }
    }

    @Override
    public void updateExhibition(Exhibition exhibition) {
        if (exhibition == null || exhibition.getTitle() == null)
            return;
        // For in-memory, find and replace
        for (Gallery g : galleryService.getAllGalleries()) {
            g.getExhibitions().removeIf(e -> exhibition.getTitle().equals(e.getTitle()));
        }
        standaloneExhibitions.remove(exhibition.getTitle());
        createExhibition(exhibition);
    }

    @Override
    public void deleteExhibition(String title) {
        if (title == null)
            return;
        for (Gallery g : galleryService.getAllGalleries()) {
            g.getExhibitions().removeIf(e -> title.equals(e.getTitle()));
        }
        standaloneExhibitions.remove(title);
    }
}
