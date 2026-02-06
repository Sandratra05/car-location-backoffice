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
('Hotel Ibis', 15.3),
('Hotel du Louvre', 20),
('Hotel Carlton', 19.8),
('Hotel Colbert', 21);
