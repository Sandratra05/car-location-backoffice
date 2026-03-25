# Algorithme d'Assignation des Réservations aux Véhicules

## Vue d'ensemble

L'algorithme assigne les réservations de transfert aux véhicules disponibles en utilisant une approche **greedy (gloutonne)** avec optimisation **best-fit** pour minimiser le gaspillage de places.

---

## Principes Clés

1. **Tri décroissant** : Les réservations sont triées par nombre de passagers (du plus grand au plus petit)
2. **Best-fit** : Chaque réservation est assignée au véhicule qui minimise le gaspillage de places
3. **Remplissage (Fill)** : Après l'assignation principale, les places restantes sont remplies avec les réservations les plus proches de la capacité disponible
4. **Split (Division)** : Si aucun véhicule ne peut contenir une réservation entière, elle est divisée

---

## Flow Principal

### Méthode : `assignByIntervals()`

```
Pour chaque intervalle de temps :
    1. Récupérer les réservations à traiter (triées par passagers décroissant)
    2. Récupérer les véhicules disponibles

    Pour chaque réservation :
        a. Chercher un leftover prioritaire (MAIN SPLIT uniquement)
        b. Sinon, prendre la plus grande réservation
        c. Trouver le meilleur véhicule (best-fit)
        d. Si véhicule trouvé → Assigner
        e. Sinon → MAIN SPLIT (diviser la réservation)
        f. Remplir les places restantes du véhicule (FILL)
```

---

## Fonctions Principales

### 1. `findBestVehicleForReservation()`

**But** : Trouver le véhicule qui minimise le gaspillage de places.

**Logique** :
```
Pour chaque véhicule disponible :
    Si capacité >= passagers demandés :
        Calculer gaspillage = capacité - passagers
        Garder le véhicule avec le plus petit gaspillage
Retourner le meilleur véhicule (ou null si aucun ne convient)
```

**Exemple** :
- Réservation : 10 passagers
- Véhicules : [5 places, 12 places, 9 places]
- Résultat : véhicule 12 places (gaspillage = 2) ✓

---

### 2. `findVehicleWithMostSpace()`

**But** : Trouver le véhicule avec la plus grande capacité (pour les splits).

**Logique** :
```
Parcourir tous les véhicules disponibles
Retourner celui avec la capacité maximale
```

**Utilisation** : Appelée quand aucun véhicule ne peut contenir la réservation entière.

---

### 3. `findNextReservationToProcess()`

**But** : Chercher un leftover prioritaire issu d'un MAIN SPLIT.

**Logique** :
```
Pour chaque réservation dans toProcess :
    Si son ID est dans splitClientIds :
        C'est un leftover d'un MAIN SPLIT
        Garder le plus grand leftover
Retourner le plus grand leftover (ou null)
```

**Important** : Seuls les leftovers des MAIN SPLITS sont prioritaires, pas ceux des FILL SPLITS.

---

### 4. `findClosestReservationToFill()`

**But** : Trouver la meilleure réservation pour remplir l'espace restant d'un véhicule.

**Logique** :
```
1. Séparer les réservations en deux groupes :
   - leftovers : clients déjà splittés (dans splitClientIds)
   - others : autres réservations

2. Chercher d'abord parmi les leftovers :
   - Réservation qui rentre exactement OU
   - La plus grande qui rentre OU
   - La plus petite (pour un split)

3. Si aucun leftover ne convient, chercher parmi les autres

Retourner la meilleure réservation trouvée
```

**Priorité** : Les restes des clients déjà partiellement assignés sont traités en premier.

---

### 5. `findBestReservationInList()`

**But** : Trouver la meilleure réservation dans une liste pour remplir un espace donné.

**Critères (par ordre de priorité)** :
1. **Exact fit** : Réservation qui rentre exactement
2. **Best fit** : La plus grande réservation qui rentre
3. **Smallest** : La plus petite réservation (nécessitera un split)

---

### 6. `insertSortedDesc()`

**But** : Insérer une réservation dans la liste en maintenant l'ordre décroissant.

**Logique** :
```
Trouver la position où insérer pour garder l'ordre décroissant
Insérer la réservation à cette position
```

**Utilisation** : Appelée après un split pour réinsérer le reste dans la liste.

---

## Types de Split

### MAIN SPLIT (Split Principal)

**Quand** : Aucun véhicule ne peut contenir la réservation entière.

**Actions** :
1. Prendre le véhicule avec le plus d'espace
2. Assigner la partie qui rentre
3. Créer un leftover avec le reste
4. **Ajouter l'ID à `splitClientIds`** → Ce leftover sera prioritaire

```java
// MAIN SPLIT : tracker pour priorisation
if (r.getIdReservation() != null) {
    splitClientIds.add(r.getIdReservation());
}
```

### FILL SPLIT (Split de Remplissage)

**Quand** : Pendant le remplissage, une réservation ne rentre pas entièrement.

**Actions** :
1. Assigner la partie qui rentre dans l'espace disponible
2. Créer un leftover avec le reste
3. **NE PAS ajouter à `splitClientIds`** → Ce leftover n'est pas prioritaire

```java
// FILL SPLIT : ne PAS tracker
// Le leftover sera traité pendant le FILL d'un autre véhicule
insertSortedDesc(toProcess, partRemaining);
```

---

## Structures de Données

### `splitClientIds` (Set<Integer>)

- Contient les IDs des réservations qui ont subi un **MAIN SPLIT**
- **Initialisé vide** à chaque intervalle (pas de report des intervalles précédents)
- Utilisé pour prioriser les leftovers dans la sélection principale

### `lastUnassignedParts` (List<Reservation>)

- Contient les réservations non assignées d'un intervalle
- Reportées à l'intervalle suivant
- **N'influencent PAS** `splitClientIds` du nouvel intervalle

### `toProcess` (List<Reservation>)

- Liste des réservations à traiter pour l'intervalle courant
- Maintenue triée par ordre décroissant de passagers
- Inclut les leftovers créés pendant l'intervalle

---

## Exemple Complet

### Données
- **Véhicules** : vehicule1(5), vehicule2(5), vehicule3(12), vehicule4(9), vehicule5(12)
- **Réservations** :
  - Client2(20) à 08:00
  - Client1(7), Client3(3), Client4(10), Client5(5) à 09:24
  - Client6(12) à 13:30

### Déroulement

#### Intervalle 08:00

| Étape | Action | Résultat |
|-------|--------|----------|
| 1 | Client2(20) → best-fit | Aucun véhicule assez grand |
| 2 | MAIN SPLIT sur vehicule3(12) | vehicule3 ← Client2(12) |
| 3 | Client2(8) ajouté à splitClientIds | Reste prioritaire |
| 4 | Client2(8) reporté à 09:24 | lastUnassignedParts |

#### Intervalle 09:24

| Étape | Action | Résultat |
|-------|--------|----------|
| 1 | toProcess = [Client4(10), Client2(8), Client1(7), Client5(5), Client3(3)] | Tri décroissant |
| 2 | splitClientIds = {} | Vide (pas de report) |
| 3 | Client4(10) → best-fit vehicule3(12) | vehicule3 ← Client4(10), reste 2 |
| 4 | FILL : Client3(3) splittée | vehicule3 ← Client3(2), Client3(1) leftover |
| 5 | Client2(8) → best-fit vehicule4(9) | vehicule4 ← Client2(8), reste 1 |
| 6 | FILL : Client3(1) | vehicule4 ← Client3(1) |
| 7 | Client1(7) → MAIN SPLIT vehicule1(5) | vehicule1 ← Client1(5), Client1(2) prioritaire |
| 8 | splitClientIds = {Client1} | Client1(2) prioritaire |
| 9 | Client1(2) prioritaire → vehicule2(5) | vehicule2 ← Client1(2), reste 3 |
| 10 | FILL : Client5(5) splittée | vehicule2 ← Client5(3), Client5(2) leftover |

### Résultat Final

```
vehicule3    Client2     12    08:00:00    09:24:00
vehicule3    Client4     10    09:24:00    13:00:00
vehicule3    Client3      2    09:24:00    13:00:00
vehicule4    Client2      8    09:24:00    13:06:00
vehicule4    Client3      1    09:24:00    13:06:00
vehicule1    Client1      5    09:24:00    13:00:00
vehicule2    Client1      2    09:24:00    13:00:00
vehicule2    Client5      3    09:24:00    13:00:00
vehicule5    Client6     12    13:30:00    17:06:00
vehicule1    Client5      2    13:30:00    17:06:00
```

---

## Points Importants

1. **Ordre décroissant maintenu** : Les grandes réservations sont toujours traitées en premier
2. **MAIN SPLIT vs FILL SPLIT** : Seuls les MAIN SPLITS créent des leftovers prioritaires
3. **Pas de report de priorité** : `splitClientIds` est réinitialisé à chaque intervalle
4. **Priorisation au FILL** : Les restes des clients déjà assignés sont remplis en priorité
