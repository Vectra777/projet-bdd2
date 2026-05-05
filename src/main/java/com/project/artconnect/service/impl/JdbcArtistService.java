package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.persistence.JdbcDisciplineDao;
import com.project.artconnect.service.ArtistService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JdbcArtistService implements ArtistService {
    private final ArtistDao artistDao;
    private final JdbcDisciplineDao disciplineDao;

    public JdbcArtistService(ArtistDao artistDao, JdbcDisciplineDao disciplineDao) {
        this.artistDao = artistDao;
        this.disciplineDao = disciplineDao;
    }

    @Override
    public List<Artist> getAllArtists() {
        return artistDao.findAll();
    }

    @Override
    public Optional<Artist> getArtistByName(String name) {
        return artistDao.findAll().stream()
                .filter(artist -> artist.getName() != null && artist.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public void createArtist(Artist artist) {
        artistDao.save(artist);
    }

    @Override
    public void updateArtist(Artist artist) {
        artistDao.update(artist);
    }

    @Override
    public void deleteArtist(String name) {
        artistDao.delete(name);
    }

    @Override
    public List<Discipline> getAllDisciplines() {
        return disciplineDao.findAll();
    }

    @Override
    public List<Artist> searchArtists(String query, String disciplineName, String city) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedDiscipline = disciplineName == null ? "" : disciplineName.trim().toLowerCase();
        String normalizedCity = city == null ? "" : city.trim().toLowerCase();

        List<Artist> base = normalizedCity.isEmpty() ? artistDao.findAll() : artistDao.findByCity(city);

        return base.stream()
                .filter(artist -> normalizedQuery.isEmpty()
                        || (artist.getName() != null && artist.getName().toLowerCase().contains(normalizedQuery)))
                .filter(artist -> normalizedDiscipline.isEmpty() || artist.getDisciplines().stream()
                        .map(Discipline::getName)
                        .filter(name -> name != null)
                        .map(String::toLowerCase)
                        .anyMatch(normalizedDiscipline::equals))
                .sorted(Comparator.comparing(Artist::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
