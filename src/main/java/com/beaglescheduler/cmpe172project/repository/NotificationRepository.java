package com.beaglescheduler.cmpe172project.repository;

import com.beaglescheduler.cmpe172project.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbc;

    public NotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Notification> ROW_MAPPER = new RowMapper<>() {
        @Override
        public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
            Notification n = new Notification();
            n.setNotificationId(rs.getLong("notification_id"));
            n.setAppointmentId(rs.getLong("appointment_id"));
            n.setUserId(rs.getLong("user_id"));
            n.setChannel(rs.getString("channel"));
            n.setNotificationType(rs.getString("notification_type"));
            n.setDeliveryStatus(rs.getString("delivery_status"));
            Timestamp sentAt = rs.getTimestamp("sent_at");
            n.setSentAt(sentAt != null ? sentAt.toLocalDateTime() : null);
            n.setPayloadJson(rs.getString("payload_json"));
            return n;
        }
    };

    public Notification save(Notification n) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO notifications (appointment_id, user_id, channel, notification_type, delivery_status, payload_json) " +
                "VALUES (?, ?, ?, ?, 'pending', ?)",
                new String[]{"notification_id"}
            );
            ps.setLong(1, n.getAppointmentId());
            ps.setLong(2, n.getUserId());
            ps.setString(3, n.getChannel());
            ps.setString(4, n.getNotificationType());
            ps.setString(5, n.getPayloadJson());
            return ps;
        }, keyHolder);
        n.setNotificationId(keyHolder.getKey().longValue());
        return n;
    }

    public List<Notification> findAll() {
        return jdbc.query(
            "SELECT * FROM notifications ORDER BY notification_id DESC",
            ROW_MAPPER
        );
    }

    public List<Notification> findByAppointment(long appointmentId) {
        return jdbc.query(
            "SELECT * FROM notifications WHERE appointment_id = ? ORDER BY notification_id DESC",
            ROW_MAPPER, appointmentId
        );
    }

    public List<Notification> findPending() {
        return jdbc.query(
            "SELECT * FROM notifications WHERE delivery_status = 'pending' ORDER BY notification_id",
            ROW_MAPPER
        );
    }

    public void markSent(long notificationId) {
        jdbc.update(
            "UPDATE notifications SET delivery_status = 'sent', sent_at = NOW() WHERE notification_id = ?",
            notificationId
        );
    }

    public void markFailed(long notificationId) {
        jdbc.update(
            "UPDATE notifications SET delivery_status = 'failed' WHERE notification_id = ?",
            notificationId
        );
    }

    public List<Notification> findByUser(long userId) {
        return jdbc.query(
            "SELECT * FROM notifications WHERE user_id = ? ORDER BY notification_id DESC",
            ROW_MAPPER, userId
        );
    }
}
