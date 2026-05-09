package com.beaglescheduler.cmpe172project.repository;

import com.beaglescheduler.cmpe172project.model.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class AppUserRepository {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public AppUserRepository(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    private static final RowMapper<AppUser> ROW_MAPPER = new RowMapper<>() {
        @Override
        public AppUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            AppUser u = new AppUser();
            u.setUserId(rs.getLong("user_id"));
            u.setName(rs.getString("name"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setRole(rs.getString("role"));
            u.setPassword(rs.getString("password"));
            return u;
        }
    };

    public Optional<AppUser> findByEmail(String email) {
        List<AppUser> results = jdbc.query(
            "SELECT user_id, name, email, phone, role, password FROM users WHERE email = ?",
            ROW_MAPPER, email
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<AppUser> findById(long userId) {
        List<AppUser> results = jdbc.query(
            "SELECT user_id, name, email, phone, role, password FROM users WHERE user_id = ?",
            ROW_MAPPER, userId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<AppUser> findByRole(String role) {
        return jdbc.query(
            "SELECT user_id, name, email, phone, role, password FROM users WHERE role = ?",
            ROW_MAPPER, role
        );
    }

    public List<AppUser> findAll() {
        return jdbc.query(
            "SELECT user_id, name, email, phone, role, password FROM users ORDER BY user_id",
            ROW_MAPPER
        );
    }

    /** Insert or return existing user by email. Returns the user with ID populated. */
    public AppUser save(AppUser user) {
        Optional<AppUser> existing = findByEmail(user.getEmail());
        if (existing.isPresent()) {
            return existing.get();
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO users (name, email, phone, role, password) VALUES (?, ?, ?, ?, ?)",
                new String[]{"user_id"}
            );
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getRole() != null ? user.getRole() : "CUSTOMER");
            String pwdToStore;
            if (user.getPassword() == null) {
                pwdToStore = null;
            } else if (user.getPassword().startsWith("{")) {
                pwdToStore = user.getPassword();
            } else {
                pwdToStore = passwordEncoder.encode(user.getPassword());
            }
            ps.setString(5, pwdToStore);
            return ps;
        }, keyHolder);
        user.setUserId(keyHolder.getKey().longValue());
        return user;
    }
}
