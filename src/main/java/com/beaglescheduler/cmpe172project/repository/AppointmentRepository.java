package com.beaglescheduler.cmpe172project.repository;

import com.beaglescheduler.cmpe172project.model.Appointment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class AppointmentRepository {

    private final JdbcTemplate jdbc;

    public AppointmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Appointment> ROW_MAPPER = new RowMapper<>() {
        @Override
        public Appointment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Appointment a = new Appointment();
            a.setAppointmentId(rs.getLong("appointment_id"));
            a.setSlotId(rs.getLong("slot_id"));
            a.setCustomerId(rs.getLong("customer_id"));
            a.setMachineId(rs.getLong("machine_id"));
            a.setStatus(rs.getString("status"));
            a.setCustomerNotes(rs.getString("customer_notes"));
            a.setBookedAt(rs.getTimestamp("booked_at").toLocalDateTime());
            a.setCustomerName(rs.getString("customer_name"));
            a.setCustomerEmail(rs.getString("customer_email"));
            a.setSerialNumber(rs.getString("serial_number"));
            a.setModelName(rs.getString("model_name"));
            a.setDailyRate(rs.getDouble("daily_rate"));
            a.setStartDate(rs.getDate("start_date").toLocalDate());
            a.setEndDate(rs.getDate("end_date").toLocalDate());
            a.setAssignedTechnicianId(rs.getLong("assigned_technician_id"));
            a.setAssignedTechnicianName(rs.getString("assigned_technician_name"));
            a.setMachineReady(rs.getBoolean("machine_ready"));
            return a;
        }
    };

    private static final String JOIN_SQL =
        "SELECT a.appointment_id, a.slot_id, a.customer_id, a.machine_id, a.status, " +
        "       a.customer_notes, a.booked_at, a.assigned_technician_id, a.machine_ready, " +
        "       u.name AS customer_name, u.email AS customer_email, " +
        "       m.serial_number, svc.model_name, svc.daily_rate, " +
        "       s.start_date, s.end_date, " +
        "       t.name AS assigned_technician_name " +
        "FROM appointments a " +
        "JOIN users u ON a.customer_id = u.user_id " +
        "JOIN machines m ON a.machine_id = m.machine_id " +
        "JOIN services svc ON m.service_id = svc.service_id " +
        "JOIN availability_slots s ON a.slot_id = s.slot_id " +
        "LEFT JOIN users t ON a.assigned_technician_id = t.user_id ";

    public Appointment save(Appointment appt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO appointments (slot_id, customer_id, machine_id, status, customer_notes) " +
                "VALUES (?, ?, ?, ?, ?)",
                new String[]{"appointment_id"}
            );
            ps.setLong(1, appt.getSlotId());
            ps.setLong(2, appt.getCustomerId());
            ps.setLong(3, appt.getMachineId());
            ps.setString(4, appt.getStatus() != null ? appt.getStatus() : "CONFIRMED");
            ps.setString(5, appt.getCustomerNotes());
            return ps;
        }, keyHolder);
        appt.setAppointmentId(keyHolder.getKey().longValue());
        return appt;
    }

    public void assignTechnician(long apptId, long techId) {
        jdbc.update("UPDATE appointments SET assigned_technician_id=? WHERE appointment_id=?",
            techId, apptId);
    }

    public void cancelAppointment(long id) {
        jdbc.update("UPDATE appointments SET status='CANCELLED' WHERE appointment_id=?", id);
    }

    public void reopenSlot(long slotId) {
        jdbc.update("UPDATE availability_slots SET is_available=TRUE WHERE slot_id=?", slotId);
    }

    public void markMachineReady(long id) {
        jdbc.update("UPDATE appointments SET machine_ready=TRUE WHERE appointment_id=?", id);
    }

    public List<Appointment> findAll() {
        return jdbc.query(JOIN_SQL + "ORDER BY a.booked_at DESC", ROW_MAPPER);
    }

    public Appointment findById(long appointmentId) {
        return jdbc.queryForObject(JOIN_SQL + "WHERE a.appointment_id = ?", ROW_MAPPER, appointmentId);
    }

    public List<Appointment> findByAssignedTechnician(long techId) {
        return jdbc.query(JOIN_SQL + "WHERE a.assigned_technician_id = ? ORDER BY a.booked_at DESC",
            ROW_MAPPER, techId);
    }

    public List<Appointment> findByCustomer(long customerId) {
        return jdbc.query(JOIN_SQL + "WHERE a.customer_id = ? ORDER BY a.booked_at DESC",
            ROW_MAPPER, customerId);
    }
}
