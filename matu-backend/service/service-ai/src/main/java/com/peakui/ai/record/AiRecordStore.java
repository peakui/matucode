package com.peakui.ai.record;

import com.peakui.ai.AiProperties;
import com.peakui.ai.model.AdminAiConversationVO;
import com.peakui.ai.model.AdminAiMessageVO;
import com.peakui.ai.model.AiConversationVO;
import com.peakui.ai.model.AiMessageVO;
import com.peakui.common.result.PageResponse;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Durable AI chat records in local MySQL.
 *
 * <p>The datasource is a private pool rather than a Spring bean on purpose: any
 * {@code DataSource} bean would make Spring Boot's {@code DataSourceAutoConfiguration}
 * back off and break the primary PostgreSQL datasource (pgvector RAG) that this
 * service still needs, and a second {@code Flyway} bean would stop the PostgreSQL
 * migrations from running. Building the pool here keeps both untouched.
 *
 * <p>The schema is versioned by Flyway from {@code classpath:db/migration-mysql}
 * (a sibling of the PostgreSQL {@code db/migration} location, so the two never
 * collide). Flyway is built programmatically against the private pool rather than
 * registered as a bean, for the same reason. Like every other method here, the
 * migration is best-effort and retried: an unreachable MySQL only logs a warning,
 * so chat keeps working and the store recovers once MySQL is back.
 */
@Slf4j
@Component
public class AiRecordStore {

    private static final int TITLE_MAX = 100;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String MIGRATION_LOCATION = "classpath:db/migration-mysql";

    private final AiProperties properties;
    private final Object schemaLock = new Object();

    private HikariDataSource dataSource;
    private JdbcTemplate jdbc;
    private boolean disabled;
    private volatile boolean schemaReady;

    public AiRecordStore(AiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        AiProperties.RecordProperties config = properties.getRecord();
        if (!config.isEnabled() || !StringUtils.hasText(config.getUrl())) {
            disabled = true;
            log.info("AI record store disabled (ai.record.datasource.url not set)");
            return;
        }
        try {
            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl(config.getUrl());
            hikari.setUsername(config.getUsername());
            hikari.setPassword(config.getPassword());
            hikari.setDriverClassName(config.getDriverClassName());
            hikari.setMaximumPoolSize(config.getMaximumPoolSize());
            hikari.setMinimumIdle(config.getMinimumIdle());
            hikari.setConnectionTimeout(config.getConnectionTimeoutMs());
            // Never block startup when MySQL is down; ensureSchema retries later.
            hikari.setInitializationFailTimeout(-1);
            hikari.setPoolName("ai-record-pool");
            dataSource = new HikariDataSource(hikari);
            jdbc = new JdbcTemplate(dataSource);
            ensureSchema();
        } catch (RuntimeException error) {
            disabled = true;
            log.warn("AI record store init failed, chat records will not be persisted: {}", error.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    /** Migrates the schema on first use and retries until MySQL is reachable. */
    private boolean ensureSchema() {
        if (schemaReady) return true;
        if (disabled || dataSource == null) return false;
        synchronized (schemaLock) {
            if (schemaReady) return true;
            try {
                // Baseline at 0 so V1 also runs against a database whose tables were
                // created by an earlier release (its CREATE TABLE IF NOT EXISTS then
                // no-ops), while a fresh database simply applies V1.
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations(MIGRATION_LOCATION)
                        .baselineOnMigrate(true)
                        .baselineVersion("0")
                        .load()
                        .migrate();
                schemaReady = true;
                log.info("AI record store ready (MySQL, Flyway)");
                return true;
            } catch (RuntimeException error) {
                log.warn("AI record store unavailable, chat records skipped: {}", error.getMessage());
                return false;
            }
        }
    }

    public void appendMessage(String userId, String conversationId, String role, String content, String scene) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(content) || !ensureSchema()) {
            return;
        }
        try {
            String title = "user".equals(role) ? truncate(content, TITLE_MAX) : null;
            jdbc.update("INSERT INTO ai_conversation(conversation_id, user_id, title, scene, message_count) " +
                            "VALUES(?,?,?,?,1) " +
                            "ON DUPLICATE KEY UPDATE message_count = message_count + 1, " +
                            "updated_at = CURRENT_TIMESTAMP, " +
                            "title = COALESCE(title, ?), scene = COALESCE(scene, ?)",
                    conversationId, userId, title, scene, title, scene);
            jdbc.update("INSERT INTO ai_message(conversation_id, user_id, role, content) VALUES(?,?,?,?)",
                    conversationId, userId, role, content);
        } catch (RuntimeException error) {
            log.warn("failed to persist AI chat record: {}", error.getMessage());
        }
    }

    public PageResponse<AiConversationVO> listConversations(String userId, String keyword, long pageNum, long pageSize) {
        long page = normalizePageNum(pageNum);
        long size = normalizePageSize(pageSize);
        if (!ensureSchema()) return PageResponse.of(page, size, 0, List.of());
        boolean hasKeyword = StringUtils.hasText(keyword);
        try {
            String filter = hasKeyword ? " AND title LIKE ?" : "";
            Object[] filterArgs = hasKeyword ? new Object[] { userId, likePattern(keyword) } : new Object[] { userId };
            Long total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ai_conversation WHERE user_id=?" + filter, Long.class, filterArgs);
            List<Object> queryArgs = new java.util.ArrayList<>(List.of(filterArgs));
            queryArgs.add(size);
            queryArgs.add((page - 1) * size);
            List<AiConversationVO> records = jdbc.query(
                    "SELECT id, conversation_id, title, scene, message_count, created_at, updated_at " +
                            "FROM ai_conversation WHERE user_id=?" + filter +
                            " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                    AiRecordStore::mapConversation, queryArgs.toArray());
            return PageResponse.of(page, size, total == null ? 0 : total, records);
        } catch (RuntimeException error) {
            log.warn("failed to list AI conversations: {}", error.getMessage());
            return PageResponse.of(page, size, 0, List.of());
        }
    }

    /** Deletes a conversation and its messages atomically. Returns false if the conversation did not exist. */
    public boolean deleteConversation(String userId, String conversationId) {
        if (!StringUtils.hasText(conversationId) || !ensureSchema()) return false;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteMessages = connection.prepareStatement(
                         "DELETE FROM ai_message WHERE user_id=? AND conversation_id=?");
                 PreparedStatement deleteConversation = connection.prepareStatement(
                         "DELETE FROM ai_conversation WHERE user_id=? AND conversation_id=?")) {
                deleteMessages.setString(1, userId);
                deleteMessages.setString(2, conversationId);
                deleteMessages.executeUpdate();
                deleteConversation.setString(1, userId);
                deleteConversation.setString(2, conversationId);
                int removed = deleteConversation.executeUpdate();
                connection.commit();
                return removed > 0;
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            log.warn("failed to delete AI conversation: {}", error.getMessage());
            return false;
        }
    }

    /**
     * Renames a conversation. {@code updated_at = updated_at} keeps the column's
     * ON UPDATE CURRENT_TIMESTAMP from firing, so a rename does not reorder the list.
     */
    public boolean renameConversation(String userId, String conversationId, String title) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(title) || !ensureSchema()) return false;
        try {
            return jdbc.update("UPDATE ai_conversation SET title=?, updated_at=updated_at " +
                            "WHERE user_id=? AND conversation_id=?",
                    truncate(title.trim(), TITLE_MAX), userId, conversationId) > 0;
        } catch (RuntimeException error) {
            log.warn("failed to rename AI conversation: {}", error.getMessage());
            return false;
        }
    }

    public PageResponse<AiMessageVO> listMessages(String userId, String conversationId, long pageNum, long pageSize) {
        long page = normalizePageNum(pageNum);
        long size = normalizePageSize(pageSize);
        if (!ensureSchema()) return PageResponse.of(page, size, 0, List.of());
        try {
            Long total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ai_message WHERE user_id=? AND conversation_id=?",
                    Long.class, userId, conversationId);
            List<AiMessageVO> records = jdbc.query(
                    "SELECT id, role, content, created_at FROM ai_message " +
                            "WHERE user_id=? AND conversation_id=? ORDER BY id ASC LIMIT ? OFFSET ?",
                    AiRecordStore::mapMessage, userId, conversationId, size, (page - 1) * size);
            return PageResponse.of(page, size, total == null ? 0 : total, records);
        } catch (RuntimeException error) {
            log.warn("failed to list AI messages: {}", error.getMessage());
            return PageResponse.of(page, size, 0, List.of());
        }
    }

    /** Global (all users) conversation listing for the admin console. */
    public PageResponse<AdminAiConversationVO> listAllConversations(String keyword, long pageNum, long pageSize) {
        long page = normalizePageNum(pageNum);
        long size = normalizePageSize(pageSize);
        if (!ensureSchema()) return PageResponse.of(page, size, 0, List.of());
        boolean hasKeyword = StringUtils.hasText(keyword);
        try {
            String filter = hasKeyword ? " WHERE title LIKE ? OR conversation_id LIKE ? OR user_id LIKE ?" : "";
            Object[] filterArgs = hasKeyword ? new Object[] { likePattern(keyword), likePattern(keyword), likePattern(keyword) } : new Object[0];
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM ai_conversation" + filter, Long.class, filterArgs);
            String sql = "SELECT id, conversation_id, user_id, title, scene, message_count, created_at, updated_at " +
                    "FROM ai_conversation" + filter + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?";
            List<Object> queryArgs = new java.util.ArrayList<>(List.of(filterArgs));
            queryArgs.add(size);
            queryArgs.add((page - 1) * size);
            List<AdminAiConversationVO> records = jdbc.query(sql, AiRecordStore::mapAdminConversation, queryArgs.toArray());
            return PageResponse.of(page, size, total == null ? 0 : total, records);
        } catch (RuntimeException error) {
            log.warn("failed to list all AI conversations: {}", error.getMessage());
            return PageResponse.of(page, size, 0, List.of());
        }
    }

    /** Global (all users) message listing for the admin console. */
    public PageResponse<AdminAiMessageVO> listAllMessages(String conversationId, String userId, long pageNum, long pageSize) {
        long page = normalizePageNum(pageNum);
        long size = normalizePageSize(pageSize);
        if (!ensureSchema()) return PageResponse.of(page, size, 0, List.of());
        boolean hasConversation = StringUtils.hasText(conversationId);
        boolean hasUser = StringUtils.hasText(userId);
        try {
            StringBuilder filter = new StringBuilder();
            List<Object> args = new java.util.ArrayList<>();
            if (hasConversation) {
                filter.append(filter.length() == 0 ? " WHERE " : " AND ").append("conversation_id=?");
                args.add(conversationId);
            }
            if (hasUser) {
                filter.append(filter.length() == 0 ? " WHERE " : " AND ").append("user_id=?");
                args.add(userId);
            }
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM ai_message" + filter,
                    Long.class, args.toArray());
            String sql = "SELECT id, conversation_id, user_id, role, content, created_at FROM ai_message" + filter +
                    " ORDER BY id ASC LIMIT ? OFFSET ?";
            List<Object> queryArgs = new java.util.ArrayList<>(args);
            queryArgs.add(size);
            queryArgs.add((page - 1) * size);
            List<AdminAiMessageVO> records = jdbc.query(sql, AiRecordStore::mapAdminMessage, queryArgs.toArray());
            return PageResponse.of(page, size, total == null ? 0 : total, records);
        } catch (RuntimeException error) {
            log.warn("failed to list all AI messages: {}", error.getMessage());
            return PageResponse.of(page, size, 0, List.of());
        }
    }

    /** True when the durable record store is configured and reachable. */
    public boolean isAvailable() {
        return !disabled && ensureSchema();
    }

    /** Used for the ownership check. Degraded store returns true so callers get an empty page, not a spurious 404. */
    public boolean conversationExists(String userId, String conversationId) {
        if (!StringUtils.hasText(conversationId) || !ensureSchema()) return true;
        try {
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ai_conversation WHERE user_id=? AND conversation_id=?",
                    Long.class, userId, conversationId);
            return count != null && count > 0;
        } catch (RuntimeException error) {
            log.warn("failed to check AI conversation: {}", error.getMessage());
            return true;
        }
    }

    private static AiConversationVO mapConversation(ResultSet rs, int rowNum) throws SQLException {
        return new AiConversationVO(
                rs.getLong("id"),
                rs.getString("conversation_id"),
                rs.getString("title"),
                rs.getString("scene"),
                rs.getInt("message_count"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private static AiMessageVO mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new AiMessageVO(
                rs.getLong("id"),
                rs.getString("role"),
                rs.getString("content"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private static AdminAiConversationVO mapAdminConversation(ResultSet rs, int rowNum) throws SQLException {
        return new AdminAiConversationVO(
                rs.getLong("id"),
                rs.getString("conversation_id"),
                rs.getString("user_id"),
                rs.getString("title"),
                rs.getString("scene"),
                rs.getInt("message_count"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private static AdminAiMessageVO mapAdminMessage(ResultSet rs, int rowNum) throws SQLException {
        return new AdminAiMessageVO(
                rs.getLong("id"),
                rs.getString("conversation_id"),
                rs.getString("user_id"),
                rs.getString("role"),
                rs.getString("content"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private static String likePattern(String keyword) {
        return "%" + keyword + "%";
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static long normalizePageNum(long pageNum) {
        return pageNum < 1 ? 1 : pageNum;
    }

    private static long normalizePageSize(long pageSize) {
        if (pageSize < 1) return 20;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
