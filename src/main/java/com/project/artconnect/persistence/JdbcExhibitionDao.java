package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcExhibitionDao implements ExhibitionDao {
    private static final String FIND_ALL_SQL = """
            SELECT
                e.exhibition_id,
                e.title,
                e.start_date,
                e.end_date,
                e.description,
                e.curator_name,
                e.theme,
                g.gallery_id,
                g.name AS gallery_name,
                g.address,
                g.owner_name,
                g.opening_hours,
                g.contact_phone,
                g.rating,
                g.website
            FROM exhibition e
            JOIN gallery g ON g.gallery_id = e.gallery_id
            ORDER BY e.start_date, e.title
            """;
    private static final String LOAD_ARTWORKS_SQL = """
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
            INSERT INTO exhibition (title, start_date, end_date, description, curator_name, theme, gallery_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE exhibition
            SET start_date = ?, end_date = ?, description = ?, curator_name = ?, theme = ?, gallery_id = ?
            WHERE title = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM exhibition
            WHERE title = ?
            """;
    private static final String FIND_EXHIBITION_ID_SQL = """
            SELECT exhibition_id
            FROM exhibition
            WHERE title = ?
            """;
    private static final String FIND_GALLERY_ID_SQL = """
            SELECT gallery_id
            FROM gallery
            WHERE name = ?
            """;
    private static final String FIND_ARTWORK_ID_SQL = """
            SELECT artwork_id
            FROM artwork
            WHERE title = ?
            """;
    private static final String DELETE_EXHIBITION_ARTWORKS_SQL = """
            DELETE FROM exhibition_artwork
            WHERE exhibition_id = ?
            """;
    private static final String INSERT_EXHIBITION_ARTWORK_SQL = """
            INSERT INTO exhibition_artwork (exhibition_id, artwork_id)
            VALUES (?, ?)
            """;

    @Override
    public List<Exhibition> findAll() {
        Map<Integer, Exhibition> exhibitionsById = new LinkedHashMap<>();

        try (Connection connection = ConnectionManager.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int exhibitionId = resultSet.getInt("exhibition_id");
                    exhibitionsById.put(exhibitionId, mapExhibition(resultSet));
                }
            }

            loadArtworks(connection, exhibitionsById);
            return new ArrayList<>(exhibitionsById.values());
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to load exhibitions", e);
        }
    }

    @Override
    public void save(Exhibition exhibition) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                int galleryId = JdbcSupport.requireId(connection, FIND_GALLERY_ID_SQL, exhibition.getGallery().getName(), "Gallery");
                bindExhibitionForInsert(statement, exhibition, galleryId);
                statement.executeUpdate();

                int exhibitionId;
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("Unable to retrieve generated exhibition_id.");
                    }
                    exhibitionId = generatedKeys.getInt(1);
                }

                syncExhibitionArtworks(connection, exhibitionId, exhibition.getArtworks());
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to save exhibition", e);
        }
    }

    @Override
    public void update(Exhibition exhibition) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                int galleryId = JdbcSupport.requireId(connection, FIND_GALLERY_ID_SQL, exhibition.getGallery().getName(), "Gallery");
                bindExhibitionForUpdate(statement, exhibition, galleryId);
                statement.executeUpdate();

                int exhibitionId = JdbcSupport.requireId(connection, FIND_EXHIBITION_ID_SQL, exhibition.getTitle(), "Exhibition");
                syncExhibitionArtworks(connection, exhibitionId, exhibition.getArtworks());
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to update exhibition", e);
        }
    }

    @Override
    public void delete(String title) {
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setString(1, title);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to delete exhibition", e);
        }
    }

    private void loadArtworks(Connection connection, Map<Integer, Exhibition> exhibitionsById) throws SQLException {
        if (exhibitionsById.isEmpty()) {
            return;
        }

        Map<Integer, Artist> artistsById = new LinkedHashMap<>();
        Map<Integer, Artwork> artworksById = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_ARTWORKS_SQL);
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

    private void syncExhibitionArtworks(Connection connection, int exhibitionId, List<Artwork> artworks) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_EXHIBITION_ARTWORKS_SQL)) {
            deleteStatement.setInt(1, exhibitionId);
            deleteStatement.executeUpdate();
        }

        if (artworks == null || artworks.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_EXHIBITION_ARTWORK_SQL)) {
            for (Artwork artwork : artworks) {
                int artworkId = JdbcSupport.requireId(connection, FIND_ARTWORK_ID_SQL, artwork.getTitle(), "Artwork");
                insertStatement.setInt(1, exhibitionId);
                insertStatement.setInt(2, artworkId);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private Exhibition mapExhibition(ResultSet resultSet) throws SQLException {
        Gallery gallery = new Gallery();
        gallery.setName(resultSet.getString("gallery_name"));
        gallery.setAddress(resultSet.getString("address"));
        gallery.setOwnerName(resultSet.getString("owner_name"));
        gallery.setOpeningHours(resultSet.getString("opening_hours"));
        gallery.setContactPhone(resultSet.getString("contact_phone"));
        gallery.setRating(resultSet.getDouble("rating"));
        gallery.setWebsite(resultSet.getString("website"));

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
        return exhibition;
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

    private void bindExhibitionForInsert(PreparedStatement statement, Exhibition exhibition, int galleryId)
            throws SQLException {
        statement.setString(1, exhibition.getTitle());
        statement.setDate(2, exhibition.getStartDate() == null ? null : java.sql.Date.valueOf(exhibition.getStartDate()));
        statement.setDate(3, exhibition.getEndDate() == null ? null : java.sql.Date.valueOf(exhibition.getEndDate()));
        statement.setString(4, exhibition.getDescription());
        statement.setString(5, exhibition.getCuratorName());
        statement.setString(6, exhibition.getTheme());
        statement.setInt(7, galleryId);
    }

    private void bindExhibitionForUpdate(PreparedStatement statement, Exhibition exhibition, int galleryId)
            throws SQLException {
        statement.setDate(1, exhibition.getStartDate() == null ? null : java.sql.Date.valueOf(exhibition.getStartDate()));
        statement.setDate(2, exhibition.getEndDate() == null ? null : java.sql.Date.valueOf(exhibition.getEndDate()));
        statement.setString(3, exhibition.getDescription());
        statement.setString(4, exhibition.getCuratorName());
        statement.setString(5, exhibition.getTheme());
        statement.setInt(6, galleryId);
        statement.setString(7, exhibition.getTitle());
    }
}
