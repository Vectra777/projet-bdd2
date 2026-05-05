package com.project.artconnect.dao;

import com.project.artconnect.model.Artwork;
import java.util.List;

public interface ArtworkDao {
    List<Artwork> findAll();

    void save(Artwork artwork);

    void update(Artwork artwork);

    void delete(String title);

    List<Artwork> findByArtistName(String artistName);
}
