package com.example.entity;

import com.example.config.DbConnection;
import com.example.repository.ParametreRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.repository.DistanceRepository;
import java.util.Optional;
import com.example.service.VehiculeService;

public class Reservation {

    private Integer idReservation;
    private Integer nbPassager;
    private Timestamp dateHeureArrivee;
    private Hotel hotel;
    private String idClient;

    public Reservation() {}

    public Reservation(Integer idReservation, Integer nbPassager,
                       Timestamp dateHeureArrivee, Hotel hotel, String idClient) {
        this.idReservation = idReservation;
        this.nbPassager = nbPassager;
        this.dateHeureArrivee = dateHeureArrivee;
        this.hotel = hotel;
        this.idClient = idClient;
    }

    public Integer getIdReservation() {
        return idReservation;
    }

    public Integer getNbPassager() {
        return nbPassager;
    }

    public void setNbPassager(Integer nbPassager) {
        this.nbPassager = nbPassager;
    }

    public Timestamp getDateHeureArrivee() {
        return dateHeureArrivee;
    }

    public void setDateHeureArrivee(Timestamp dateHeureArrivee) {
        this.dateHeureArrivee = dateHeureArrivee;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) {
        this.idClient = idClient;
    }

    public void save() throws SQLException {
        String sql = """
            INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, nbPassager);
            stmt.setTimestamp(2, dateHeureArrivee);
            stmt.setInt(3, hotel.getIdHotel());
            stmt.setString(4, idClient);

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                idReservation = rs.getInt(1);
            }
        }
    }

    public static List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();

        String sql = """
            SELECT r.*, h.code, h.libelle, h.aeroport
            FROM reservation r
            JOIN hotel h ON r.id_hotel = h.id_hotel
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Hotel hotel = new Hotel(
                    rs.getInt("id_hotel"),
                    rs.getString("code"),
                    rs.getString("libelle"),
                    rs.getBoolean("aeroport")
                );

                Reservation r = new Reservation();
                r.idReservation = rs.getInt("id_reservation");
                r.nbPassager = rs.getInt("nb_passager");
                r.dateHeureArrivee = rs.getTimestamp("date_heure_arrivee");
                r.hotel = hotel;
                r.idClient = rs.getString("id_client");

                list.add(r);
            }
        }
        return list;
    }

    public static List<Reservation> findReservationsByDate(Timestamp date) throws SQLException {
        List<Reservation> list = new ArrayList<>();

        String sql = """
            SELECT r.*, h.code, h.libelle, h.aeroport
            FROM reservation r
            JOIN hotel h ON r.id_hotel = h.id_hotel
            WHERE DATE(r.date_heure_arrivee) = ?
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, new Date(date.getTime()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                        Hotel hotel = new Hotel(
                            rs.getInt("id_hotel"),
                            rs.getString("code"),
                            rs.getString("libelle"),
                            rs.getBoolean("aeroport")
                        );

                    Reservation r = new Reservation();
                    r.idReservation = rs.getInt("id_reservation");
                    r.nbPassager = rs.getInt("nb_passager");
                    r.dateHeureArrivee = rs.getTimestamp("date_heure_arrivee");
                    r.hotel = hotel;
                    r.idClient = rs.getString("id_client");

                    list.add(r);
                }
            }
        }
        return list;
    }

    /**
     * Calcule l'heure de départ du véhicule pour cette réservation en utilisant
     * le paramètre de configuration le plus récent (temps d'attente en minutes).
     * Si aucun paramètre n'est présent, retourne l'heure d'arrivée.
     */
    public Timestamp calculHeureDeDepart() throws SQLException {
        ParametreRepository repo = new ParametreRepository();
        Parametre p = repo.findLatest();
        return calculHeureDeDepart(p);
    }

    /**
     * Calcule l'heure de départ du véhicule en ajoutant le temps d'attente
     * (en minutes) indiqué par le paramètre à l'heure d'arrivée.
     * Exemple: arrivée 08:00 + attente 30min => départ 08:30.
     * Si parametre est null, retourne l'heure d'arrivée.
     */
    public Timestamp calculHeureDeDepart(Parametre parametre) {
        if (dateHeureArrivee == null) return null;
        if (parametre == null || parametre.getTempsAttenteMin() == null) {
            return dateHeureArrivee;
        }
        long waitMs = TimeUnit.MINUTES.toMillis(parametre.getTempsAttenteMin());
        return new Timestamp(dateHeureArrivee.getTime() + waitMs);
    }

    /**
     * Calcule l'heure de retour à l'aéroport en partant de l'heure de départ
     * (départ = arrivée + temps d'attente). Le calcul utilise la distance
     * entre l'aéroport et l'hôtel (via DistanceRepository) et la vitesse
     * moyenne (Parametre.vitesseMoyenneKmh). Le trajet considéré est
     * aéroport -> hôtel -> aéroport (aller-retour).
     * Retourne null si la distance ou la vitesse ne sont pas disponibles.
     */
    public Timestamp calculHeureRetour() throws SQLException {
        ParametreRepository prefRepo = new ParametreRepository();
        Parametre p = prefRepo.findLatest();
        return calculHeureRetour(p);
    }

    public Timestamp calculHeureRetour(Parametre parametre) throws SQLException {
        if (dateHeureArrivee == null) return null;

        // compute departure time
        Timestamp depart = calculHeureDeDepart(parametre);

        // find airport hotel id
        Integer airportHotelId = null;
        for (Hotel h : Hotel.findAll()) {
            if (Boolean.TRUE.equals(h.getAeroport())) {
                airportHotelId = h.getIdHotel();
                break;
            }
        }
        if (airportHotelId == null) return null;

        int hotelId = this.hotel != null ? this.hotel.getIdHotel() : -1;
        if (hotelId == -1) return null;

        DistanceRepository dr = new DistanceRepository();
        Distance d = dr.findDistance(airportHotelId, hotelId);
        if (d == null || d.getKilometre() == null) return null;

        if (parametre == null || parametre.getVitesseMoyenneKmh() == null) return null;

        BigDecimal km = d.getKilometre();
        BigDecimal vitesse = parametre.getVitesseMoyenneKmh(); // km/h

        if (vitesse.compareTo(BigDecimal.ZERO) <= 0) return null;

        // time (hours) = km / vitesse ; convert to milliseconds and double for precision
        double hoursOneWay = km.divide(vitesse, 6, BigDecimal.ROUND_HALF_UP).doubleValue();
        long travelMsOneWay = (long) (hoursOneWay * 3600.0 * 1000.0);
        long totalTravelMs = travelMsOneWay * 2L; // there and back

        return new Timestamp(depart.getTime() + totalTravelMs);
    }

    public Timestamp calculHeureRetourTotal(List<Reservation> resa) throws SQLException {
        if (resa == null || resa.isEmpty()) return null;

        // Trouver l'heure d'arrivée la plus tôt parmi les réservations
        Timestamp earliestArrival = null;
        for (Reservation r : resa) {
            if (r.getDateHeureArrivee() != null) {
                if (earliestArrival == null || r.getDateHeureArrivee().before(earliestArrival)) {
                    earliestArrival = r.getDateHeureArrivee();
                }
            }
        }
        if (earliestArrival == null) return null;

        // Créer une réservation temporaire pour calculer l'heure de départ
        Reservation temp = new Reservation();
        temp.setDateHeureArrivee(earliestArrival);

        ParametreRepository prefRepo = new ParametreRepository();
        Parametre p = prefRepo.findLatest();
        Timestamp depart = temp.calculHeureDeDepart(p);

        // Calculer la distance totale en utilisant VehiculeService
        VehiculeService vs = new VehiculeService();
        BigDecimal totalKm = vs.calculTotalDistance(resa);

        if (totalKm == null || totalKm.compareTo(BigDecimal.ZERO) <= 0) return depart;

        if (p == null || p.getVitesseMoyenneKmh() == null) return depart;

        BigDecimal vitesse = p.getVitesseMoyenneKmh();
        if (vitesse.compareTo(BigDecimal.ZERO) <= 0) return depart;

        // Temps en heures = km / vitesse
        // double hours = totalKm.divide(vitesse, 6, BigDecimal.ROUND_HALF_UP).doubleValue();
        // long travelMs = (long) (hours * 3600.0 * 1000.0);
        // long travelMs = Math.round(hours * 3600.0 * 1000.0);
        BigDecimal seconds = totalKm
                .divide(vitesse, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(3600));

        long travelMs = seconds
                .multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        // Arrondir au minute le plus proche
        long travelMinutes = Math.round(travelMs / 60000.0);
        long travelMsRounded = travelMinutes * 60000;

        return new Timestamp(depart.getTime() + travelMsRounded);
    }

    /**
     * Retourne la liste des réservations dont l'heure d'arrivée est comprise
     * entre la date passée en argument et cette date + le temps d'attente
     * (temps d'attente pris depuis le dernier enregistrement de la table
     * `parametre`).
     *
     * @param dateHeure date de départ de la fenêtre
     * @return liste des réservations dans la fenêtre
     * @throws SQLException en cas d'erreur BD
     */
    public static List<Reservation> getReservationsDansTA(Timestamp dateHeure) throws SQLException {
        if (dateHeure == null) throw new IllegalArgumentException("dateHeure ne peut pas être null");

        ParametreRepository prefRepo = new ParametreRepository();
        Parametre p = prefRepo.findLatest();
        if (p == null || p.getTempsAttenteMin() == null) {
            return new ArrayList<>();
        }

        long waitMs = TimeUnit.MINUTES.toMillis(p.getTempsAttenteMin());
        Timestamp end = new Timestamp(dateHeure.getTime() + waitMs);

        List<Reservation> list = new ArrayList<>();

        String sql = """
            SELECT r.*, h.code, h.libelle, h.aeroport
            FROM reservation r
            JOIN hotel h ON r.id_hotel = h.id_hotel
            WHERE r.date_heure_arrivee BETWEEN ? AND ?
            ORDER BY r.date_heure_arrivee
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, dateHeure);
            stmt.setTimestamp(2, end);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Hotel hotel = new Hotel(
                        rs.getInt("id_hotel"),
                        rs.getString("code"),
                        rs.getString("libelle"),
                        rs.getBoolean("aeroport")
                    );

                    Reservation r = new Reservation();
                    r.idReservation = rs.getInt("id_reservation");
                    r.nbPassager = rs.getInt("nb_passager");
                    r.dateHeureArrivee = rs.getTimestamp("date_heure_arrivee");
                    r.hotel = hotel;
                    r.idClient = rs.getString("id_client");

                    list.add(r);
                }
            }
        }

        return list;
    }

    /**
     * Calcule l'heure de départ commune de tous les véhicules pour la fenêtre
     * commençant à `dateHeure` et se terminant à `dateHeure + temps_attente`.
     *
     * Algorithme:
     * - Récupère les réservations via `getReservationsDansTA(dateHeure)`
     * - Trie par `dateHeureArrivee` et prend la plus récente (dernière)
     * - Si un `Parametre` existe, utilise `calculHeureDeDepart(param)` sur
     *   cette réservation (ajoute le temps d'attente). Sinon retourne la
     *   `dateHeureArrivee` la plus récente.
     *
     * @param dateHeure début de la fenêtre
     * @return Timestamp de départ commun des véhicules, ou null si aucune réservation
     * @throws SQLException en cas d'erreur BD
     */
    public static Timestamp getHeureDepartAvecTA(Timestamp dateHeure) throws SQLException {
        if (dateHeure == null) throw new IllegalArgumentException("dateHeure ne peut pas être null");

        List<Reservation> list = getReservationsDansTA(dateHeure);
        if (list == null || list.isEmpty()) return null;

        // Trier par date d'arrivée croissante et prendre la dernière (la plus récente)
        list.sort((a, b) -> {
            Timestamp ta = a != null ? a.getDateHeureArrivee() : null;
            Timestamp tb = b != null ? b.getDateHeureArrivee() : null;
            if (ta == null && tb == null) return 0;
            if (ta == null) return -1;
            if (tb == null) return 1;
            return ta.compareTo(tb);
        });

        Reservation latest = list.get(list.size() - 1);
        if (latest == null || latest.getDateHeureArrivee() == null) return null;

        return latest.getDateHeureArrivee();
    }
}
