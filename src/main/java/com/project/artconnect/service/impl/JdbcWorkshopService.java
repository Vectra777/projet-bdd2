package com.project.artconnect.service.impl;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.persistence.JdbcSupport;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcWorkshopService implements WorkshopService {
    private static final String FIND_WORKSHOP_ID_SQL = """
            SELECT workshop_id
            FROM workshop
            WHERE title = ?
            """;
    private static final String FIND_MEMBER_ID_SQL = """
            SELECT member_id
            FROM community_member
            WHERE email = ? OR name = ?
            ORDER BY member_id
            LIMIT 1
            """;
    private static final String INSERT_BOOKING_SQL = """
            INSERT INTO booking (workshop_id, member_id, payment_status)
            VALUES (?, ?, 'PENDING')
            """;

    private final WorkshopDao workshopDao;

    public JdbcWorkshopService(WorkshopDao workshopDao) {
        this.workshopDao = workshopDao;
    }

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        return workshopDao.findAll().stream()
                .filter(workshop -> workshop.getTitle() != null && workshop.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        if (workshop == null || workshop.getTitle() == null || member == null) {
            return;
        }

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int workshopId = JdbcSupport.requireId(connection, FIND_WORKSHOP_ID_SQL, workshop.getTitle(), "Workshop");
                int memberId = findMemberId(connection, member);

                try (PreparedStatement statement = connection.prepareStatement(INSERT_BOOKING_SQL)) {
                    statement.setInt(1, workshopId);
                    statement.setInt(2, memberId);
                    statement.executeUpdate();
                }

                connection.commit();
                member.addBooking(new Booking(workshop, member));
            } catch (SQLException e) {
                JdbcSupport.rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to create booking", e);
        }
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        return member == null ? List.of() : member.getBookings();
    }

    private int findMemberId(Connection connection, CommunityMember member) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_MEMBER_ID_SQL)) {
            statement.setString(1, member.getEmail());
            statement.setString(2, member.getName());
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        throw new SQLException("Community member not found for booking.");
    }
}
