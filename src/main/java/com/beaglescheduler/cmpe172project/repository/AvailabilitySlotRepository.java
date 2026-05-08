package com.beaglescheduler.cmpe172project.repository;

import com.beaglescheduler.cmpe172project.model.AvailabilitySlot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class AvailabilitySlotRepository {

    private final JdbcTemplate jdbc;

    public AvailabilitySlotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AvailabilitySlot> ROW_MAPPER = new RowMapper<>() {
        @Override
        public AvailabilitySlot mapRow(ResultSet rs, int rowNum) throws SQLException {
            AvailabilitySlot slot = new AvailabilitySlot();
            slot.setSlotId(rs.getLong("slot_id"));
            slot.setMachineId(rs.getLong("machine_id"));
            slot.setStartDate(rs.getDate("start_date").toLocalDate());
            slot.setEndDate(rs.getDate("end_date").toLocalDate());
            slot.setAvailable(rs.getBoolean("is_available"));
            slot.setSerialNumber(rs.getString("serial_number"));
            slot.setModelName(rs.getString("model_name"));
            slot.setDailyRate(rs.getDouble("daily_rate"));
            return slot;
        }
    };

    public List<AvailabilitySlot> findAvailable() {
        return jdbc.query(
            "SELECT s.slot_id, s.machine_id, s.start_date, s.end_date, " +
            "       s.is_available, m.serial_number, svc.model_name, svc.daily_rate " +
            "FROM availability_slots s " +
            "JOIN machines m ON s.machine_id = m.machine_id " +
            "JOIN services svc ON m.service_id = svc.service_id " +
            "WHERE s.is_available = TRUE ORDER BY s.start_date, m.serial_number",
            ROW_MAPPER
        );
    }

    public Optional<AvailabilitySlot> findById(long slotId) {
        List<AvailabilitySlot> results = jdbc.query(
            "SELECT s.slot_id, s.machine_id, s.start_date, s.end_date, " +
            "       s.is_available, m.serial_number, svc.model_name, svc.daily_rate " +
            "FROM availability_slots s " +
            "JOIN machines m ON s.machine_id = m.machine_id " +
            "JOIN services svc ON m.service_id = svc.service_id " +
            "WHERE s.slot_id = ?",
            ROW_MAPPER, slotId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /** Atomically marks slot unavailable. Returns rows updated (0 = already taken). */
    public int markUnavailable(long slotId) {
        return jdbc.update(
            "UPDATE availability_slots SET is_available = FALSE WHERE slot_id = ? AND is_available = TRUE",
            slotId
        );
    }
}
