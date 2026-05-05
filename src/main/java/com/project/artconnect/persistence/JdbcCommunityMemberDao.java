package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.model.Review;
import com.project.artconnect.model.Workshop;
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

public class JdbcCommunityMemberDao implements CommunityMemberDao {
    private static final String FIND_ALL_SQL = """
            SELECT member_id, name, email, birth_year, phone, city, membership_type
            FROM community_member
            ORDER BY name
            """;
    private static final String LOAD_DISCIPLINES_SQL = """
            SELECT md.member_id, d.name
            FROM member_discipline md
            JOIN discipline d ON d.discipline_id = md.discipline_id
            ORDER BY d.name
            """;
    private static final String LOAD_BOOKINGS_SQL = """
            SELECT
                b.member_id,
                b.booking_date,
                b.payment_status,
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
            FROM booking b
            JOIN workshop w ON w.workshop_id = b.workshop_id
            JOIN artist ar ON ar.artist_id = w.instructor_id
            ORDER BY b.booking_date
            """;
    private static final String LOAD_REVIEWS_SQL = """
            SELECT
                r.member_id,
                r.rating,
                r.comment,
                r.review_date,
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
            FROM review r
            JOIN artwork aw ON aw.artwork_id = r.artwork_id
            JOIN artist ar ON ar.artist_id = aw.artist_id
            ORDER BY r.review_date
            """;

    @Override
    public Optional<CommunityMember> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadMembers().get(id.intValue()));
    }

    @Override
    public List<CommunityMember> findAll() {
        return new ArrayList<>(loadMembers().values());
    }

    private Map<Integer, CommunityMember> loadMembers() {
        Map<Integer, CommunityMember> membersById = new LinkedHashMap<>();

        try (Connection connection = ConnectionManager.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    membersById.put(resultSet.getInt("member_id"), mapMember(resultSet));
                }
            }

            loadDisciplines(connection, membersById);
            loadBookings(connection, membersById);
            loadReviews(connection, membersById);
            return membersById;
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to load community members", e);
        }
    }

    private void loadDisciplines(Connection connection, Map<Integer, CommunityMember> membersById) throws SQLException {
        if (membersById.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(LOAD_DISCIPLINES_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                CommunityMember member = membersById.get(resultSet.getInt("member_id"));
                if (member != null) {
                    member.getFavoriteDisciplines().add(new Discipline(resultSet.getString("name")));
                }
            }
        }
    }

    private void loadBookings(Connection connection, Map<Integer, CommunityMember> membersById) throws SQLException {
        if (membersById.isEmpty()) {
            return;
        }

        Map<Integer, Artist> artistsById = new LinkedHashMap<>();
        Map<Integer, Workshop> workshopsById = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_BOOKINGS_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                CommunityMember member = membersById.get(resultSet.getInt("member_id"));
                if (member == null) {
                    continue;
                }

                int artistId = resultSet.getInt("artist_id");
                Artist instructor = artistsById.get(artistId);
                if (instructor == null) {
                    instructor = mapArtist(resultSet);
                    artistsById.put(artistId, instructor);
                }
                int workshopId = resultSet.getInt("workshop_id");
                Workshop workshop = workshopsById.get(workshopId);
                if (workshop == null) {
                    workshop = mapWorkshop(resultSet, instructor);
                    workshopsById.put(workshopId, workshop);
                }

                Booking booking = new Booking();
                booking.setMember(member);
                booking.setWorkshop(workshop);
                booking.setPaymentStatus(resultSet.getString("payment_status"));
                var bookingDate = resultSet.getTimestamp("booking_date");
                booking.setBookingDate(bookingDate == null ? null : bookingDate.toLocalDateTime());
                member.getBookings().add(booking);
            }
        }
    }

    private void loadReviews(Connection connection, Map<Integer, CommunityMember> membersById) throws SQLException {
        if (membersById.isEmpty()) {
            return;
        }

        Map<Integer, Artist> artistsById = new LinkedHashMap<>();
        Map<Integer, Artwork> artworksById = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(LOAD_REVIEWS_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                CommunityMember member = membersById.get(resultSet.getInt("member_id"));
                if (member == null) {
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

                Review review = new Review();
                review.setReviewer(member);
                review.setArtwork(artwork);
                review.setRating(resultSet.getInt("rating"));
                review.setComment(resultSet.getString("comment"));
                var reviewDate = resultSet.getDate("review_date");
                review.setReviewDate(reviewDate == null ? null : reviewDate.toLocalDate());
                member.getReviews().add(review);
            }
        }
    }

    private CommunityMember mapMember(ResultSet resultSet) throws SQLException {
        CommunityMember member = new CommunityMember();
        member.setName(resultSet.getString("name"));
        member.setEmail(resultSet.getString("email"));
        int birthYear = resultSet.getInt("birth_year");
        member.setBirthYear(resultSet.wasNull() ? null : birthYear);
        member.setPhone(resultSet.getString("phone"));
        member.setCity(resultSet.getString("city"));
        member.setMembershipType(resultSet.getString("membership_type"));
        return member;
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

    private Workshop mapWorkshop(ResultSet resultSet, Artist instructor) throws SQLException {
        Workshop workshop = new Workshop();
        workshop.setTitle(resultSet.getString("title"));
        var date = resultSet.getTimestamp("date");
        workshop.setDate(date == null ? null : date.toLocalDateTime());
        workshop.setDurationMinutes(resultSet.getInt("duration_minutes"));
        workshop.setMaxParticipants(resultSet.getInt("max_participants"));
        workshop.setPrice(resultSet.getDouble("price"));
        workshop.setLocation(resultSet.getString("location"));
        workshop.setDescription(resultSet.getString("description"));
        workshop.setLevel(resultSet.getString("level"));
        workshop.setInstructor(instructor);
        return workshop;
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
