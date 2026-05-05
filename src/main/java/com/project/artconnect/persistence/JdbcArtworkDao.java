package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.ArtworkTag;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC implementation for ArtworkDao.
 */
public class JdbcArtworkDao implements ArtworkDao {
    private static final String BASE_SELECT = """
            SELECT
                aw.artwork_id,
                aw.title,
                aw.creation_year,
                aw.type,
                aw.medium,
                aw.dimensions,
                aw.description,
                aw.price,
                aw.status,
                ar.artist_id AS artist_id,
                ar.name AS artist_name,
                ar.bio AS artist_bio,
                ar.birth_year AS artist_birth_year,
                ar.contact_email AS artist_contact_email,
                ar.phone AS artist_phone,
                ar.city AS artist_city,
                ar.website AS artist_website,
                ar.social_media AS artist_social_media,
                ar.is_active AS artist_is_active
            FROM artwork aw
            JOIN artist ar ON ar.artist_id = aw.artist_id
            """;
    private static final String FIND_ALL_SQL = BASE_SELECT + " ORDER BY aw.title";
    private static final String FIND_BY_ARTIST_SQL = BASE_SELECT + " WHERE ar.name = ? ORDER BY aw.title";
    private static final String LOAD_TAGS_SQL = """
            SELECT atm.artwork_id, t.name
            FROM artwork_tag_map atm
            JOIN artwork_tag t ON t.tag_id = atm.tag_id
            ORDER BY t.name
            """;
    private static final String INSERT_SQL = """
            INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE artwork
            SET creation_year = ?, type = ?, medium = ?, dimensions = ?, description = ?, price = ?, status = ?, artist_id = ?
            WHERE title = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM artwork
            WHERE title = ?
            """;
    private static final String FIND_ARTIST_ID_SQL = """
            SELECT artist_id
            FROM artist
            WHERE name = ?
            """;
    private static final String FIND_ARTWORK_ID_SQL = """
            SELECT artwork_id
            FROM artwork
            WHERE title = ?
            """;
    private static final String FIND_TAG_ID_SQL = """
            SELECT tag_id
            FROM artwork_tag
            WHERE name = ?
            """;
    private static final String DELETE_TAGS_SQL = """
            DELETE FROM artwork_tag_map
            WHERE artwork_id = ?
            """;
    private static final String INSERT_TAG_SQL = """
            INSERT INTO artwork_tag_map (artwork_id, tag_id)
            VALUES (?, ?)
            """;

    @Override
    public List<Artwork> findAll() {
        return queryArtworks(FIND_ALL_SQL, null);
    }

    @Override
    public void save(Artwork artwork) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                int artistId = JdbcSupport.requireId(connection, FIND_ARTIST_ID_SQL, artwork.getArtist().getName(), "Artist");
                bindArtworkForInsert(statement, artwork, artistId);
                statement.executeUpdate();

                int artworkId;
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("Unable to retrieve generated artwork_id.");
                    }
                    artworkId = generatedKeys.getInt(1);
                }

                syncTags(connection, artworkId, artwork.getTags());
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to save artwork", e);
        }
    }

    @Override
    public void update(Artwork artwork) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                int artistId = JdbcSupport.requireId(connection, FIND_ARTIST_ID_SQL, artwork.getArtist().getName(), "Artist");
                bindArtworkForUpdate(statement, artwork, artistId);
                statement.executeUpdate();

                int artworkId = JdbcSupport.requireId(connection, FIND_ARTWORK_ID_SQL, artwork.getTitle(), "Artwork");
                syncTags(connection, artworkId, artwork.getTags());
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to update artwork", e);
        }
    }

    @Override
    public void delete(String title) {
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setString(1, title);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to delete artwork", e);
        }
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        return queryArtworks(FIND_BY_ARTIST_SQL, artistName);
    }

    private List<Artwork> queryArtworks(String sql, String parameter) {
        Map<Integer, Artwork> artworksById = new LinkedHashMap<>();
        Map<Integer, Artist> artistsById = new LinkedHashMap<>();

        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter != null) {
                statement.setString(1, parameter);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int artistId = resultSet.getInt("artist_id");
                    Artist artist = artistsById.get(artistId);
                    if (artist == null) {
                        artist = mapArtist(resultSet);
                        artistsById.put(artistId, artist);
                    }

                    int artworkId = resultSet.getInt("artwork_id");
                    Artwork artwork = mapArtwork(resultSet, artist);
                    artist.addArtwork(artwork);
                    artworksById.put(artworkId, artwork);
                }
            }

            loadTags(connection, artworksById);
            return new ArrayList<>(artworksById.values());
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to load artworks", e);
        }
    }

    private void loadTags(Connection connection, Map<Integer, Artwork> artworksById) throws SQLException {
        if (artworksById.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(LOAD_TAGS_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Artwork artwork = artworksById.get(resultSet.getInt("artwork_id"));
                if (artwork != null) {
                    artwork.getTags().add(new ArtworkTag(resultSet.getString("name")));
                }
            }
        }
    }

    private void syncTags(Connection connection, int artworkId, List<ArtworkTag> tags) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_TAGS_SQL)) {
            deleteStatement.setInt(1, artworkId);
            deleteStatement.executeUpdate();
        }

        if (tags == null || tags.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_TAG_SQL)) {
            for (ArtworkTag tag : tags) {
                int tagId = JdbcSupport.requireId(connection, FIND_TAG_ID_SQL, tag.getName(), "Artwork tag");
                insertStatement.setInt(1, artworkId);
                insertStatement.setInt(2, tagId);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private void bindArtworkForInsert(PreparedStatement statement, Artwork artwork, int artistId) throws SQLException {
        statement.setString(1, artwork.getTitle());
        JdbcSupport.setNullableInteger(statement, 2, artwork.getCreationYear());
        statement.setString(3, artwork.getType());
        statement.setString(4, artwork.getMedium());
        statement.setString(5, artwork.getDimensions());
        statement.setString(6, artwork.getDescription());
        statement.setDouble(7, artwork.getPrice());
        statement.setString(8, artwork.getStatus() == null ? Artwork.Status.FOR_SALE.name() : artwork.getStatus().name());
        statement.setInt(9, artistId);
    }

    private void bindArtworkForUpdate(PreparedStatement statement, Artwork artwork, int artistId) throws SQLException {
        JdbcSupport.setNullableInteger(statement, 1, artwork.getCreationYear());
        statement.setString(2, artwork.getType());
        statement.setString(3, artwork.getMedium());
        statement.setString(4, artwork.getDimensions());
        statement.setString(5, artwork.getDescription());
        statement.setDouble(6, artwork.getPrice());
        statement.setString(7, artwork.getStatus() == null ? Artwork.Status.FOR_SALE.name() : artwork.getStatus().name());
        statement.setInt(8, artistId);
        statement.setString(9, artwork.getTitle());
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
