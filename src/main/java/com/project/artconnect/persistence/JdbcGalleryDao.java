package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcGalleryDao implements GalleryDao {
    private static final String FIND_ALL_GALLERIES_SQL = """
            SELECT gallery_id, name, address, owner_name, opening_hours, contact_phone, rating, website
            FROM gallery
            ORDER BY name
            """;
    private static final String LOAD_EXHIBITIONS_SQL = """
            SELECT exhibition_id, title, start_date, end_date, description, curator_name, theme, gallery_id
            FROM exhibition
            ORDER BY start_date, title
            """;
    private static final String LOAD_EXHIBITION_ARTWORKS_SQL = """
            SELECT
                ea.exhibition_id,
                aw.artwork_id,
                aw.title,
                aw.creation_year,
                aw.type,
                aw.medium,
                aw.dimensions,
                aw.description,
                aw.price,
                aw.status,
                ar.artist_id,
                ar.name AS artist_name,
                ar.bio AS artist_bio,
                ar.birth_year AS artist_birth_year,
                ar.contact_email AS artist_contact_email,
                ar.phone AS artist_phone,
                ar.city AS artist_city,
                ar.website AS artist_website,
                ar.social_media AS artist_social_media,
                ar.is_active AS artist_is_active
            FROM exhibition_artwork ea
            JOIN artwork aw ON aw.artwork_id = ea.artwork_id
            JOIN artist ar ON ar.artist_id = aw.artist_id
            ORDER BY ea.exhibition_id, aw.title
            """;
    private static final String INSERT_SQL = """
            INSERT INTO gallery (name, address, owner_name, opening_hours, contact_phone, rating, website)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE gallery
            SET address = ?, owner_name = ?, opening_hours = ?, contact_phone = ?, rating = ?, website = ?
            WHERE name = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM gallery
            WHERE name = ?
            """;
    private static final String DELETE_EXHIBITIONS_SQL = """
            DELETE e
            FROM exhibition e
            JOIN gallery g ON g.gallery_id = e.gallery_id
            WHERE g.name = ?
            """;

    @Override
    public Optional<Gallery> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadGalleries().get(id.intValue()));
    }

    @Override
    public List<Gallery> findAll() {
        return new ArrayList<>(loadGalleries().values());
    }

    @Override
    public void save(Gallery gallery) {
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, gallery.getName());
            statement.setString(2, gallery.getAddress());
            statement.setString(3, gallery.getOwnerName());
            statement.setString(4, gallery.getOpeningHours());
            statement.setString(5, gallery.getContactPhone());
            statement.setDouble(6, gallery.getRating());
            statement.setString(7, gallery.getWebsite());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to save gallery", e);
        }
    }

    @Override
    public void update(Gallery gallery) {
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, gallery.getAddress());
            statement.setString(2, gallery.getOwnerName());
            statement.setString(3, gallery.getOpeningHours());
            statement.setString(4, gallery.getContactPhone());
            statement.setDouble(5, gallery.getRating());
            statement.setString(6, gallery.getWebsite());
            statement.setString(7, gallery.getName());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to update gallery", e);
        }
    }

    @Override
    public void delete(String name) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteExhibitionsStatement = connection.prepareStatement(DELETE_EXHIBITIONS_SQL);
                    PreparedStatement deleteGalleryStatement = connection.prepareStatement(DELETE_SQL)) {
                deleteExhibitionsStatement.setString(1, name);
                deleteExhibitionsStatement.executeUpdate();

                deleteGalleryStatement.setString(1, name);
                deleteGalleryStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to delete gallery", e);
        }
    }

    private Map<Integer, Gallery> loadGalleries() {
        Map<Integer, Gallery> galleriesById = new LinkedHashMap<>();

        try (Connection connection = ConnectionManager.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_GALLERIES_SQL);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    galleriesById.put(resultSet.getInt("gallery_id"), mapGallery(resultSet));
                }
            }

            Map<Integer, Exhibition> exhibitionsById = loadExhibitions(connection, galleriesById);
            loadExhibitionArtworks(connection, exhibitionsById);
            return galleriesById;
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to load galleries", e);
        }
    }

    private Map<Integer, Exhibition> loadExhibitions(Connection connection, Map<Integer, Gallery> galleriesById)
            throws SQLException {
        Map<Integer, Exhibition> exhibitionsById = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_EXHIBITIONS_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Gallery gallery = galleriesById.get(resultSet.getInt("gallery_id"));
                if (gallery == null) {
                    continue;
                }

                Exhibition exhibition = new Exhibition();
                exhibition.setTitle(resultSet.getString("title"));
                var startDate = resultSet.getDate("start_date");
                var endDate = resultSet.getDate("end_date");
                exhibition.setStartDate(startDate == null ? null : startDate.toLocalDate());
                exhibition.setEndDate(endDate == null ? null : endDate.toLocalDate());
                exhibition.setDescription(resultSet.getString("description"));
                exhibition.setCuratorName(resultSet.getString("curator_name"));
                exhibition.setTheme(resultSet.getString("theme"));
                exhibition.setGallery(gallery);
                gallery.addExhibition(exhibition);
                exhibitionsById.put(resultSet.getInt("exhibition_id"), exhibition);
            }
        }

        return exhibitionsById;
    }

    private void loadExhibitionArtworks(Connection connection, Map<Integer, Exhibition> exhibitionsById) throws SQLException {
        if (exhibitionsById.isEmpty()) {
            return;
        }

        Map<Integer, Artist> artistsById = new LinkedHashMap<>();
        Map<Integer, Artwork> artworksById = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_EXHIBITION_ARTWORKS_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Exhibition exhibition = exhibitionsById.get(resultSet.getInt("exhibition_id"));
                if (exhibition == null) {
                    continue;
                }

                int artistId = resultSet.getInt("artist_id");
                Artist artist = artistsById.get(artistId);
                if (artist == null) {
                    artist = mapArtist(resultSet);
                    artistsById.put(artistId, artist);
                }

                int artworkId = resultSet.getInt("artwork_id");
                Artwork artwork = artworksById.get(artworkId);
                if (artwork == null) {
                    artwork = mapArtwork(resultSet, artist);
                    artist.addArtwork(artwork);
                    artworksById.put(artworkId, artwork);
                }
                exhibition.getArtworks().add(artwork);
            }
        }
    }

    private Gallery mapGallery(ResultSet resultSet) throws SQLException {
        Gallery gallery = new Gallery();
        gallery.setName(resultSet.getString("name"));
        gallery.setAddress(resultSet.getString("address"));
        gallery.setOwnerName(resultSet.getString("owner_name"));
        gallery.setOpeningHours(resultSet.getString("opening_hours"));
        gallery.setContactPhone(resultSet.getString("contact_phone"));
        gallery.setRating(resultSet.getDouble("rating"));
        gallery.setWebsite(resultSet.getString("website"));
        return gallery;
    }

    private Artist mapArtist(ResultSet resultSet) throws SQLException {
        Artist artist = new Artist();
        artist.setName(resultSet.getString("artist_name"));
        artist.setBio(resultSet.getString("artist_bio"));
        int birthYear = resultSet.getInt("artist_birth_year");
        artist.setBirthYear(resultSet.wasNull() ? null : birthYear);
        artist.setContactEmail(resultSet.getString("artist_contact_email"));
        artist.setPhone(resultSet.getString("artist_phone"));
        artist.setCity(resultSet.getString("artist_city"));
        artist.setWebsite(resultSet.getString("artist_website"));
        artist.setSocialMedia(resultSet.getString("artist_social_media"));
        artist.setActive(resultSet.getBoolean("artist_is_active"));
        return artist;
    }

    private Artwork mapArtwork(ResultSet resultSet, Artist artist) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setTitle(resultSet.getString("title"));
        int creationYear = resultSet.getInt("creation_year");
        artwork.setCreationYear(resultSet.wasNull() ? null : creationYear);
        artwork.setType(resultSet.getString("type"));
        artwork.setMedium(resultSet.getString("medium"));
        artwork.setDimensions(resultSet.getString("dimensions"));
        artwork.setDescription(resultSet.getString("description"));
        artwork.setPrice(resultSet.getDouble("price"));
        artwork.setStatus(Artwork.Status.valueOf(resultSet.getString("status")));
        artwork.setArtist(artist);
        return artwork;
    }
}
