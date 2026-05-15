package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
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
 * JDBC implementation for ArtistDao.
 */
public class JdbcArtistDao implements ArtistDao {
    private static final String FIND_ALL_SQL = """
            SELECT artist_id, name, bio, birth_year, contact_email, phone, city, website, social_media, is_active
            FROM artist
            ORDER BY name
            """;
    private static final String FIND_BY_CITY_SQL = """
            SELECT artist_id, name, bio, birth_year, contact_email, phone, city, website, social_media, is_active
            FROM artist
            WHERE LOWER(city) = LOWER(?)
            ORDER BY name
            """;
    private static final String LOAD_DISCIPLINES_SQL = """
            SELECT ad.artist_id, d.name
            FROM artist_discipline ad
            JOIN discipline d ON d.discipline_id = ad.discipline_id
            ORDER BY d.name
            """;
    private static final String INSERT_SQL = """
            INSERT INTO artist (name, bio, birth_year, contact_email, phone, city, website, social_media, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE artist
            SET bio = ?, birth_year = ?, contact_email = ?, phone = ?, city = ?, website = ?, social_media = ?, is_active = ?
            WHERE name = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM artist
            WHERE name = ?
            """;
    private static final String FIND_ARTWORK_IDS_BY_ARTIST_SQL = """
            SELECT aw.artwork_id
            FROM artwork aw
            JOIN artist ar ON ar.artist_id = aw.artist_id
            WHERE ar.name = ?
            """;
    private static final String DELETE_EXHIBITION_LINKS_SQL = """
            DELETE FROM exhibition_artwork
            WHERE artwork_id = ?
            """;
    private static final String DELETE_WORKSHOPS_SQL = """
            DELETE w
            FROM workshop w
            JOIN artist ar ON ar.artist_id = w.instructor_id
            WHERE ar.name = ?
            """;
    private static final String FIND_ARTIST_ID_SQL = """
            SELECT artist_id
            FROM artist
            WHERE name = ?
            """;
    private static final String FIND_DISCIPLINE_ID_SQL = """
            SELECT discipline_id
            FROM discipline
            WHERE name = ?
            """;
    private static final String DELETE_DISCIPLINES_SQL = """
            DELETE FROM artist_discipline
            WHERE artist_id = ?
            """;
    private static final String INSERT_DISCIPLINE_SQL = """
            INSERT INTO artist_discipline (artist_id, discipline_id)
            VALUES (?, ?)
            """;

    @Override
    public List<Artist> findAll() {
        return queryArtists(FIND_ALL_SQL, null);
    }

    @Override
    public void save(Artist artist) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                bindArtistForInsert(statement, artist);
                statement.executeUpdate();

                int artistId;
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("Unable to retrieve generated artist_id.");
                    }
                    artistId = generatedKeys.getInt(1);
                }

                syncDisciplines(connection, artistId, artist.getDisciplines());
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to save artist", e);
        }
    }

    @Override
    public void update(Artist artist) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindArtistForUpdate(statement, artist);
                statement.executeUpdate();

                int artistId = JdbcSupport.requireId(connection, FIND_ARTIST_ID_SQL, artist.getName(), "Artist");
                syncDisciplines(connection, artistId, artist.getDisciplines());
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to update artist", e);
        }
    }

    @Override
    public void delete(String artistName) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteLinksStatement = connection.prepareStatement(DELETE_EXHIBITION_LINKS_SQL);
                    PreparedStatement deleteWorkshopsStatement = connection.prepareStatement(DELETE_WORKSHOPS_SQL);
                    PreparedStatement deleteArtistStatement = connection.prepareStatement(DELETE_SQL)) {
                for (Integer artworkId : findArtworkIdsByArtist(connection, artistName)) {
                    deleteLinksStatement.setInt(1, artworkId);
                    deleteLinksStatement.addBatch();
                }
                deleteLinksStatement.executeBatch();

                deleteWorkshopsStatement.setString(1, artistName);
                deleteWorkshopsStatement.executeUpdate();

                deleteArtistStatement.setString(1, artistName);
                deleteArtistStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to delete artist", e);
        }
    }

    @Override
    public List<Artist> findByCity(String city) {
        return queryArtists(FIND_BY_CITY_SQL, city);
    }

    private List<Integer> findArtworkIdsByArtist(Connection connection, String artistName) throws SQLException {
        List<Integer> artworkIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_ARTWORK_IDS_BY_ARTIST_SQL)) {
            statement.setString(1, artistName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    artworkIds.add(resultSet.getInt("artwork_id"));
                }
            }
        }
        return artworkIds;
    }

    private List<Artist> queryArtists(String sql, String parameter) {
        Map<Integer, Artist> artistsById = new LinkedHashMap<>();

        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter != null) {
                statement.setString(1, parameter);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int artistId = resultSet.getInt("artist_id");
                    artistsById.put(artistId, mapArtist(resultSet));
                }
            }

            loadDisciplines(connection, artistsById);
            return new ArrayList<>(artistsById.values());
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to load artists", e);
        }
    }

    private void loadDisciplines(Connection connection, Map<Integer, Artist> artistsById) throws SQLException {
        if (artistsById.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(LOAD_DISCIPLINES_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Artist artist = artistsById.get(resultSet.getInt("artist_id"));
                if (artist != null) {
                    artist.getDisciplines().add(new Discipline(resultSet.getString("name")));
                }
            }
        }
    }

    private void syncDisciplines(Connection connection, int artistId, List<Discipline> disciplines) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_DISCIPLINES_SQL)) {
            deleteStatement.setInt(1, artistId);
            deleteStatement.executeUpdate();
        }

        if (disciplines == null || disciplines.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_DISCIPLINE_SQL)) {
            for (Discipline discipline : disciplines) {
                int disciplineId = JdbcSupport.requireId(
                        connection,
                        FIND_DISCIPLINE_ID_SQL,
                        discipline.getName(),
                        "Discipline");
                insertStatement.setInt(1, artistId);
                insertStatement.setInt(2, disciplineId);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private void bindArtistForInsert(PreparedStatement statement, Artist artist) throws SQLException {
        statement.setString(1, artist.getName());
        statement.setString(2, artist.getBio());
        JdbcSupport.setNullableInteger(statement, 3, artist.getBirthYear());
        statement.setString(4, artist.getContactEmail());
        statement.setString(5, artist.getPhone());
        statement.setString(6, artist.getCity());
        statement.setString(7, artist.getWebsite());
        statement.setString(8, artist.getSocialMedia());
        statement.setBoolean(9, artist.isActive());
    }

    private void bindArtistForUpdate(PreparedStatement statement, Artist artist) throws SQLException {
        statement.setString(1, artist.getBio());
        JdbcSupport.setNullableInteger(statement, 2, artist.getBirthYear());
        statement.setString(3, artist.getContactEmail());
        statement.setString(4, artist.getPhone());
        statement.setString(5, artist.getCity());
        statement.setString(6, artist.getWebsite());
        statement.setString(7, artist.getSocialMedia());
        statement.setBoolean(8, artist.isActive());
        statement.setString(9, artist.getName());
    }

    private Artist mapArtist(ResultSet resultSet) throws SQLException {
        Artist artist = new Artist();
        artist.setName(resultSet.getString("name"));
        artist.setBio(resultSet.getString("bio"));
        int birthYear = resultSet.getInt("birth_year");
        artist.setBirthYear(resultSet.wasNull() ? null : birthYear);
        artist.setContactEmail(resultSet.getString("contact_email"));
        artist.setPhone(resultSet.getString("phone"));
        artist.setCity(resultSet.getString("city"));
        artist.setWebsite(resultSet.getString("website"));
        artist.setSocialMedia(resultSet.getString("social_media"));
        artist.setActive(resultSet.getBoolean("is_active"));
        return artist;
    }
}
