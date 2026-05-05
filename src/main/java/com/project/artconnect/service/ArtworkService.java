package com.project.artconnect.service;

import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Artist;
import java.util.List;
import java.util.Optional;

public interface ArtworkService {
    List<Artwork> getAllArtworks();

    Optional<Artwork> getArtworkByTitle(String title);

    List<Artwork> getArtworksByArtist(Artist artist);

    void createArtwork(Artwork artwork);

    void updateArtwork(Artwork artwork);

    void deleteArtwork(String title);
}
