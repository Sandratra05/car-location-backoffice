package com.example.entity;

import com.example.config.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class Assignation {

	private Integer id;
	private Vehicule vehicule;
	private Reservation reservation;
	private Timestamp dateDepart;
	private Timestamp dateRetour;

	public Assignation() {}

	public Assignation(Integer id, Vehicule vehicule, Reservation reservation,
					   Timestamp dateDepart, Timestamp dateRetour) {
		this.id = id;
		this.vehicule = vehicule;
		this.reservation = reservation;
		this.dateDepart = dateDepart;
		this.dateRetour = dateRetour;
	}

	public Integer getId() {
		return id;
	}

	public Vehicule getVehicule() {
		return vehicule;
	}

	public void setVehicule(Vehicule vehicule) {
		this.vehicule = vehicule;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	public Timestamp getDateDepart() {
		return dateDepart;
	}

	public void setDateDepart(Timestamp dateDepart) {
		this.dateDepart = dateDepart;
	}

	public Timestamp getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(Timestamp dateRetour) {
		this.dateRetour = dateRetour;
	}

	public void save() throws SQLException {
		String sql = "INSERT INTO assignation (id_vehicule, id_reservation, date_depart, date_retour) VALUES (?, ?, ?, ?)";

		try (Connection conn = DbConnection.getInstance().getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			if (vehicule == null || vehicule.getId() == null) {
				throw new IllegalStateException("vehicule doit être défini avant save()");
			}
			if (reservation == null || reservation.getIdReservation() == null) {
				throw new IllegalStateException("reservation doit être définie avant save()");
			}

			stmt.setLong(1, vehicule.getId());
			stmt.setInt(2, reservation.getIdReservation());
			stmt.setTimestamp(3, dateDepart);
			stmt.setTimestamp(4, dateRetour);

			stmt.executeUpdate();

			try (ResultSet rs = stmt.getGeneratedKeys()) {
				if (rs.next()) {
					this.id = rs.getInt(1);
				}
			}
		}
	}
}
