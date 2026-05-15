-- ============================================================
-- ArtConnect - Étape 3.2 : Vues & Index
-- MySQL 8.0+  |  Cours TI603 – INGE1
-- ============================================================

USE artconnect_db;

-- ============================================================
-- VUES
-- ============================================================

-- Vue 1 : vue_artiste_public
-- Masque les données sensibles (email, téléphone, réseaux sociaux)
CREATE OR REPLACE VIEW vue_artiste_public AS
SELECT
  artist_id,
  name,
  bio,
  birth_year,
  city,
  website,
  is_active
FROM artist
WHERE is_active = TRUE;


-- Vue 2 : vue_oeuvres_notees
-- Agrège les oeuvres avec leur note moyenne et le nombre d'avis
CREATE OR REPLACE VIEW vue_oeuvres_notees AS
SELECT
  a.artwork_id,
  a.title,
  a.type,
  a.medium,
  a.price,
  a.status,
  ar.name              AS artiste,
  ar.city              AS ville_artiste,
  ROUND(AVG(r.rating), 2) AS note_moyenne,
  COUNT(r.review_id)   AS nb_avis
FROM artwork a
JOIN artist ar ON a.artist_id = ar.artist_id
LEFT JOIN review r ON a.artwork_id = r.artwork_id
GROUP BY
  a.artwork_id, a.title, a.type, a.medium,
  a.price, a.status, ar.name, ar.city;


-- Vue 3 : vue_exposition_resume
-- Résumé des expositions avec leur galerie et le nombre d'oeuvres présentées
CREATE OR REPLACE VIEW vue_exposition_resume AS
SELECT
  e.exhibition_id,
  e.title          AS exposition,
  e.start_date,
  e.end_date,
  e.theme,
  e.curator_name,
  g.name           AS galerie,
  g.address        AS adresse_galerie,
  COUNT(ea.artwork_id) AS nb_oeuvres
FROM exhibition e
JOIN gallery g ON e.gallery_id = g.gallery_id
LEFT JOIN exhibition_artwork ea ON e.exhibition_id = ea.exhibition_id
GROUP BY
  e.exhibition_id, e.title, e.start_date, e.end_date,
  e.theme, e.curator_name, g.name, g.address;


-- Vue 4 : vue_inscriptions_membres
-- Vue métier des réservations avec toutes les infos utiles
CREATE OR REPLACE VIEW vue_inscriptions_membres AS
SELECT
  b.booking_id,
  cm.name             AS membre,
  cm.membership_type,
  w.title             AS atelier,
  w.date              AS date_atelier,
  w.level             AS niveau,
  w.price             AS prix_atelier,
  b.booking_date,
  b.payment_status
FROM booking b
JOIN community_member cm ON b.member_id  = cm.member_id
JOIN workshop         w  ON b.workshop_id = w.workshop_id;


-- Vue 5 : vue_stats_artistes
-- Tableau de bord par artiste : oeuvres, CA, note moyenne
CREATE OR REPLACE VIEW vue_stats_artistes AS
SELECT
  ar.artist_id,
  ar.name,
  ar.city,
  ar.is_active,
  COUNT(DISTINCT a.artwork_id)                                        AS nb_oeuvres_total,
  SUM(CASE WHEN a.status = 'SOLD'      THEN 1 ELSE 0 END)            AS nb_vendues,
  SUM(CASE WHEN a.status = 'FOR_SALE'  THEN 1 ELSE 0 END)            AS nb_a_vendre,
  SUM(CASE WHEN a.status = 'EXHIBITED' THEN 1 ELSE 0 END)            AS nb_exposees,
  COALESCE(SUM(CASE WHEN a.status = 'SOLD' THEN a.price ELSE 0 END), 0) AS ca_ventes,
  ROUND(AVG(r.rating), 2)                                             AS note_moyenne_oeuvres
FROM artist ar
LEFT JOIN artwork a ON ar.artist_id = a.artist_id
LEFT JOIN review  r ON a.artwork_id  = r.artwork_id
GROUP BY ar.artist_id, ar.name, ar.city, ar.is_active;


-- ============================================================
-- INDEX
-- ============================================================

CREATE INDEX idx_artwork_artist_id        ON artwork(artist_id);
CREATE INDEX idx_exhibition_gallery_id    ON exhibition(gallery_id);
CREATE INDEX idx_booking_workshop_member  ON booking(workshop_id, member_id);
CREATE INDEX idx_review_artwork_id        ON review(artwork_id);
CREATE INDEX idx_artwork_status           ON artwork(status);
CREATE INDEX idx_workshop_date            ON workshop(date);
