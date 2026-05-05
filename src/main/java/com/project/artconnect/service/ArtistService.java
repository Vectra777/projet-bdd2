package com.project.artconnect.service;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import java.util.List;
import java.util.Optional;

public interface ArtistService {
    List<Artist> getAllArtists();

    Optional<Artist> getArtistByName(String name);

    void createArtist(Artist artist);

    void updateArtist(Artist artist);

    void deleteArtist(String name);

    List<Discipline> getAllDisciplines();

    List<Artist> searchArtists(String query, String disciplineName, String city);
}
