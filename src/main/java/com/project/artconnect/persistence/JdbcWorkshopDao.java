package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcWorkshopDao implements WorkshopDao {
    private static final String FIND_ALL_SQL = """
            SELECT
                w.workshop_id,
                w.title,
                w.date,
                w.duration_minutes,
                w.max_participants,
                w.price,
                w.location,
                w.description,
                w.level,
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
            FROM workshop w
            JOIN artist ar ON ar.artist_id = w.instructor_id
            ORDER BY w.date, w.title
            """;
    private static final String INSERT_SQL = """
            INSERT INTO workshop (title, date, duration_minutes, max_participants, price, location, description, level, instructor_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE workshop
            SET date = ?, duration_minutes = ?, max_participants = ?, price = ?, location = ?, description = ?, level = ?, instructor_id = ?
            WHERE title = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM workshop
            WHERE title = ?
            """;
    private static final String FIND_ARTIST_ID_SQL = """
            SELECT artist_id
            FROM artist
            WHERE name = ?
            """;

    @Override
    public Optional<Workshop> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadWorkshops().get(id.intValue()));
    }

    @Override
    public List<Workshop> findAll() {
        return new ArrayList<>(loadWorkshops().values());
    }

    @Override
    public void save(Workshop workshop) {
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            int instructorId = JdbcSupport.requireId(connection, FIND_ARTIST_ID_SQL,
                    workshop.getInstructor().getName(), "Artist");
            statement.setString(1, workshop.getTitle());
            statement.setTimestamp(2, workshop.getDate() == null ? null : Timestamp.valueOf(workshop.getDate()));
            statement.setInt(3, workshop.getDurationMinutes());
            statement.setInt(4, workshop.getMaxParticipants());
            statement.setDouble(5, workshop.getPrice());
            statement.setString(6, workshop.getLocation());
            statement.setString(7, workshop.getDescription());
            statement.setString(8, workshop.getLevel());
            statement.setInt(9, instructorId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to save workshop", e);
        }
    }

    @Override
    public void update(Workshop workshop) {
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            int instructorId = JdbcSupport.requireId(connection, FIND_ARTIST_ID_SQL,
                    workshop.getInstructor().getName(), "Artist");
            statement.setTimestamp(1, workshop.getDate() == null ? null : Timestamp.valueOf(workshop.getDate()));
            statement.setInt(2, workshop.getDurationMinutes());
            statement.setInt(3, workshop.getMaxParticipants());
            statement.setDouble(4, workshop.getPrice());
            statement.setString(5, workshop.getLocation());
            statement.setString(6, workshop.getDescription());
            statement.setString(7, workshop.getLevel());
            statement.setInt(8, instructorId);
            statement.setString(9, workshop.getTitle());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to update workshop", e);
        }
    }

    @Override
    public void delete(String title) {
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setString(1, title);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to delete workshop", e);
        }
    }

    private Map<Integer, Workshop> loadWorkshops() {
        Map<Integer, Workshop> workshopsById = new LinkedHashMap<>();
        Map<Integer, Artist> artistsById = new LinkedHashMap<>();

        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int artistId = resultSet.getInt("artist_id");
                Artist artist = artistsById.get(artistId);
                if (artist == null) {
                    artist = mapArtist(resultSet);
                    artistsById.put(artistId, artist);
                }
                workshopsById.put(resultSet.getInt("workshop_id"), mapWorkshop(resultSet, artist));
            }
            return workshopsById;
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to load workshops", e);
        }
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

    private Workshop mapWorkshop(ResultSet resultSet, Artist artist) throws SQLException {
        Workshop workshop = new Workshop();
        workshop.setTitle(resultSet.getString("title"));
        var timestamp = resultSet.getTimestamp("date");
        workshop.setDate(timestamp == null ? null : timestamp.toLocalDateTime());
        workshop.setDurationMinutes(resultSet.getInt("duration_minutes"));
        workshop.setMaxParticipants(resultSet.getInt("max_participants"));
        workshop.setPrice(resultSet.getDouble("price"));
        workshop.setLocation(resultSet.getString("location"));
        workshop.setDescription(resultSet.getString("description"));
        workshop.setLevel(resultSet.getString("level"));
        workshop.setInstructor(artist);
        return workshop;
    }
}
