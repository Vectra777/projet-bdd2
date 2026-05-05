package com.project.artconnect.service;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.model.Exhibition;
import java.util.List;
import java.util.Optional;

public interface GalleryService {
    List<Gallery> getAllGalleries();

    Optional<Gallery> getGalleryByName(String name);

    List<Exhibition> getExhibitionsByGallery(Gallery gallery);
}
