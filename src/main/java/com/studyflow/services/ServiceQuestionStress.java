package com.studyflow.services;

import com.studyflow.models.QuestionStress;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceQuestionStress {

    private Connection cnx;

    private String tableName;
    private String idColumn;
    private String positionColumn;
    private String textColumn;
    private String activeColumn;
    private String createdColumn;
    private String updatedColumn;

    public ServiceQuestionStress() {
        ensureConnectionAvailable();
        if (cnx != null) {
            resolveSchema();
        }
    }

    public List<QuestionStress> findAll() {
        ensureConnectionAvailable();
        if (!schemaReady()) {
            return new ArrayList<>();
        }

        List<QuestionStress> list = new ArrayList<>();
        String sql = """
                SELECT %s AS id,
                       %s AS position,
                       %s AS question_text,
                       %s AS is_active,
                       %s AS created_at,
                       %s AS updated_at
                FROM %s
                """.formatted(
                q(idColumn),
                q(positionColumn),
                q(textColumn),
                q(activeColumn),
                nullableColumn(createdColumn),
                nullableColumn(updatedColumn),
                q(tableName)
        );

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ServiceQuestionStress] findAll error: " + e.getMessage());
        }

        return list;
    }

    public QuestionStress findById(int id) {
        ensureConnectionAvailable();
        if (!schemaReady()) {
            return null;
        }

        String sql = """
                SELECT %s AS id,
                       %s AS position,
                       %s AS question_text,
                       %s AS is_active,
                       %s AS created_at,
                       %s AS updated_at
                FROM %s
                WHERE %s = ?
                """.formatted(
                q(idColumn),
                q(positionColumn),
                q(textColumn),
                q(activeColumn),
                nullableColumn(createdColumn),
                nullableColumn(updatedColumn),
                q(tableName),
                q(idColumn)
        );

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ServiceQuestionStress] findById error: " + e.getMessage());
        }

        return null;
    }

    public void save(QuestionStress question) {
        ensureConnectionAvailable();
        if (!schemaReady()) {
            return;
        }

        if (question.getPosition() <= 0) {
            question.setPosition(nextPosition());
        }

        boolean hasCreated = createdColumn != null;
        String sql = hasCreated
                ? "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)".formatted(
                q(tableName), q(positionColumn), q(textColumn), q(activeColumn), q(createdColumn))
                : "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)".formatted(
                q(tableName), q(positionColumn), q(textColumn), q(activeColumn));

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, question.getPosition());
            ps.setString(i++, question.getQuestionText());
            ps.setBoolean(i++, question.isActive());
            if (hasCreated) {
                ps.setTimestamp(i, Timestamp.valueOf(
                        question.getCreatedAt() != null ? question.getCreatedAt() : LocalDateTime.now()));
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    question.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ServiceQuestionStress] save error: " + e.getMessage());
        }
    }

    public void update(QuestionStress question) {
        ensureConnectionAvailable();
        if (!schemaReady()) {
            return;
        }

        boolean hasUpdated = updatedColumn != null;
        String sql = hasUpdated
                ? "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?".formatted(
                q(tableName), q(positionColumn), q(textColumn), q(activeColumn), q(updatedColumn), q(idColumn))
                : "UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?".formatted(
                q(tableName), q(positionColumn), q(textColumn), q(activeColumn), q(idColumn));

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, question.getPosition());
            ps.setString(i++, question.getQuestionText());
            ps.setBoolean(i++, question.isActive());
            if (hasUpdated) {
                ps.setTimestamp(i++, Timestamp.valueOf(
                        question.getUpdatedAt() != null ? question.getUpdatedAt() : LocalDateTime.now()));
            }
            ps.setInt(i, question.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ServiceQuestionStress] update error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        ensureConnectionAvailable();
        if (!schemaReady()) {
            return;
        }

        String sql = "DELETE FROM %s WHERE %s = ?".formatted(q(tableName), q(idColumn));
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ServiceQuestionStress] delete error: " + e.getMessage());
        }
    }

    public int nextPosition() {
        ensureConnectionAvailable();
        if (!schemaReady()) {
            return 1;
        }

        String sql = "SELECT COALESCE(MAX(%s), 0) + 1 AS next_position FROM %s"
                .formatted(q(positionColumn), q(tableName));
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("next_position");
            }
        } catch (SQLException e) {
            System.err.println("[ServiceQuestionStress] nextPosition error: " + e.getMessage());
        }

        return 1;
    }

    public void reorderByIds(List<Integer> orderedIds) {
        ensureConnectionAvailable();
        if (!schemaReady() || orderedIds == null || orderedIds.isEmpty()) {
            return;
        }

        String sql = updatedColumn != null
                ? "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?"
                .formatted(q(tableName), q(positionColumn), q(updatedColumn), q(idColumn))
                : "UPDATE %s SET %s = ? WHERE %s = ?"
                .formatted(q(tableName), q(positionColumn), q(idColumn));

        boolean previousAutoCommit;
        try {
            previousAutoCommit = cnx.getAutoCommit();
        } catch (SQLException e) {
            previousAutoCommit = true;
        }

        try {
            cnx.setAutoCommit(false);
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                for (int i = 0; i < orderedIds.size(); i++) {
                    int index = 1;
                    ps.setInt(index++, i + 1);
                    if (updatedColumn != null) {
                        ps.setTimestamp(index++, Timestamp.valueOf(LocalDateTime.now()));
                    }
                    ps.setInt(index, orderedIds.get(i));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            cnx.commit();
        } catch (SQLException e) {
            try {
                cnx.rollback();
            } catch (SQLException ignored) {
            }
            System.err.println("[ServiceQuestionStress] reorderByIds error: " + e.getMessage());
        } finally {
            try {
                cnx.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    private QuestionStress mapRow(ResultSet rs) throws SQLException {
        QuestionStress item = new QuestionStress();
        item.setId(rs.getInt("id"));
        item.setPosition(rs.getInt("position"));
        item.setQuestionText(rs.getString("question_text"));
        item.setActive(rs.getBoolean("is_active"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            item.setCreatedAt(created.toLocalDateTime());
        }

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            item.setUpdatedAt(updated.toLocalDateTime());
        }

        return item;
    }

    private void resolveSchema() {
        String[] tableCandidates = {"question_stress", "question"};
        String[] idCandidates = {"id", "id_ques"};
        String[] positionCandidates = {"position", "question_number_ques", "question_number"};
        String[] textCandidates = {"question_text_ques", "question_text"};
        String[] activeCandidates = {"is_active_ques", "is_active"};
        String[] createdCandidates = {"created_at_ques", "created_at"};
        String[] updatedCandidates = {"updated_at_ques", "updated_at"};

        for (String table : tableCandidates) {
            Set<String> columns = listColumns(table);
            if (columns.isEmpty()) {
                continue;
            }

            String id = pick(columns, idCandidates);
            String position = pick(columns, positionCandidates);
            String text = pick(columns, textCandidates);
            String active = pick(columns, activeCandidates);
            String created = pick(columns, createdCandidates);
            String updated = pick(columns, updatedCandidates);

            if (id != null && position != null && text != null && active != null) {
                tableName = table;
                idColumn = id;
                positionColumn = position;
                textColumn = text;
                activeColumn = active;
                createdColumn = created;
                updatedColumn = updated;
                return;
            }
        }

        System.err.println("[ServiceQuestionStress] No compatible schema found in table question_stress/question.");
    }

    private Set<String> listColumns(String table) {
        Set<String> columns = new HashSet<>();
        if (cnx == null) {
            return columns;
        }

        try {
            DatabaseMetaData metaData = cnx.getMetaData();
            try (ResultSet rs = metaData.getColumns(cnx.getCatalog(), null, table, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
        } catch (SQLException e) {
            System.err.println("[ServiceQuestionStress] Unable to inspect table " + table + ": " + e.getMessage());
        }

        return columns;
    }

    private String pick(Set<String> available, String[] candidates) {
        for (String candidate : candidates) {
            if (available.contains(candidate.toLowerCase())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean schemaReady() {
        return tableName != null
                && idColumn != null
                && positionColumn != null
                && textColumn != null
                && activeColumn != null;
    }

    private String q(String identifier) {
        return "`" + identifier + "`";
    }

    private String nullableColumn(String columnName) {
        return columnName == null ? "NULL" : q(columnName);
    }

    private void ensureConnectionAvailable() {
        cnx = MyDataBase.getInstance().getConnection();
        if (cnx == null) {
            System.err.println("[ServiceQuestionStress] Database connection is unavailable.");
            return;
        }

        if (!schemaReady()) {
            resolveSchema();
        }
    }
}
