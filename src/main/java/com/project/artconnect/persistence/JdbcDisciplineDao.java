package com.project.artconnect.persistence;

import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcDisciplineDao {
    private static final String FIND_ALL_SQL = """
            SELECT name
            FROM discipline
            ORDER BY name
            """;

    public List<Discipline> findAll() {
        List<Discipline> disciplines = new ArrayList<>();

        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                disciplines.add(new Discipline(resultSet.getString("name")));
            }
            return disciplines;
        } catch (SQLException e) {
            throw JdbcSupport.failure("Unable to load disciplines", e);
        }
    }
}
