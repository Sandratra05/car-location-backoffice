DROP TABLE reservation;
DROP TABLE hotel;

CREATE TABLE hotel(
   id_hotel SERIAL,
   libelle VARCHAR(50)  NOT NULL,
   distance NUMERIC(15,2)   NOT NULL,
   PRIMARY KEY(id_hotel)
);

CREATE TABLE reservation(
   id_reservation SERIAL,
   nb_passager INTEGER NOT NULL,
   date_heure_arrivee TIMESTAMP NOT NULL,
   id_hotel INTEGER NOT NULL,
   id_client VARCHAR(4) NOT NULL,
   PRIMARY KEY(id_reservation),
   FOREIGN KEY(id_hotel) REFERENCES hotel(id_hotel)
);

INSERT INTO hotel (libelle, distance) VALUES
('Colbert', 15.3),
('Novotel', 20),
('Ibis', 19.8),
('Lokanga', 21);

INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) VALUES
(11, '2026-02-05 00:01:00', 3, '4631'),
(1, '2026-02-05 23:55:00', 3, '4394'),
(2, '2026-02-09 10:17:00', 1, '8054'),
(4, '2026-02-01 15:25:00', 2, '1432'),
(4, '2026-01-28 07:11:00', 1, '7861'),
(5, '2026-01-28 07:45:00', 1, '3308'),
(13, '2026-02-28 08:25:00', 2, '4484'),
(8, '2026-02-28 13:00:00', 2, '9687'),
(7, '2026-02-15 13:00:00', 1, '6302'),
(1, '2026-02-18 22:55:00', 4, '8640');
