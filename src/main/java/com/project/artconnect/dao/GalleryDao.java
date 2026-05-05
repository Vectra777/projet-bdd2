package com.project.artconnect.dao;

import com.project.artconnect.model.Gallery;
import java.util.List;
import java.util.Optional;

public interface GalleryDao {
    Optional<Gallery> findById(Long id);

    List<Gallery> findAll();
}
