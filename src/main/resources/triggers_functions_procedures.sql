-- ============================================================
-- ArtConnect - Étape 3.3 : Triggers, Fonctions & Procédures
-- MySQL 8.0+  |  Cours TI603 – INGE1
-- ============================================================

USE artconnect_db;

-- ============================================================
-- TRIGGERS
-- ============================================================

-- Trigger 1 : trg_check_max_participants
-- Avant chaque réservation, vérifie que l'atelier n'est pas complet
DELIMITER $$

CREATE TRIGGER trg_check_max_participants
BEFORE INSERT ON booking
FOR EACH ROW
BEGIN
  DECLARE v_current_count   INT;
  DECLARE v_max_participants INT;

  SELECT COUNT(*) INTO v_current_count
  FROM booking
  WHERE workshop_id = NEW.workshop_id
    AND payment_status != 'CANCELLED';

  SELECT max_participants INTO v_max_participants
  FROM workshop
  WHERE workshop_id = NEW.workshop_id;

  IF v_current_count >= v_max_participants THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Atelier complet : nombre maximum de participants atteint.';
  END IF;
END$$

DELIMITER ;


-- Trigger 2a : trg_artwork_to_exhibited
-- À l'ajout dans exhibition_artwork -> status = 'EXHIBITED'
DELIMITER $$

CREATE TRIGGER trg_artwork_to_exhibited
AFTER INSERT ON exhibition_artwork
FOR EACH ROW
BEGIN
  UPDATE artwork
  SET status = 'EXHIBITED'
  WHERE artwork_id = NEW.artwork_id;
END$$

DELIMITER ;


-- Trigger 2b : trg_artwork_restore_status
-- Au retrait de exhibition_artwork -> status = 'FOR_SALE' si plus exposée nulle part
DELIMITER $$

CREATE TRIGGER trg_artwork_restore_status
AFTER DELETE ON exhibition_artwork
FOR EACH ROW
BEGIN
  DECLARE v_still_exhibited INT;

  SELECT COUNT(*) INTO v_still_exhibited
  FROM exhibition_artwork
  WHERE artwork_id = OLD.artwork_id;

  IF v_still_exhibited = 0 THEN
    UPDATE artwork
    SET status = 'FOR_SALE'
    WHERE artwork_id = OLD.artwork_id
      AND status     = 'EXHIBITED';
  END IF;
END$$

DELIMITER ;


-- Trigger 3a : trg_booking_audit_insert
-- Audit de toutes les créations de réservations
DELIMITER $$

CREATE TRIGGER trg_booking_audit_insert
AFTER INSERT ON booking
FOR EACH ROW
BEGIN
  INSERT INTO booking_audit (booking_id, workshop_id, member_id, action, new_status)
  VALUES (NEW.booking_id, NEW.workshop_id, NEW.member_id, 'INSERT', NEW.payment_status);
END$$

DELIMITER ;


-- Trigger 3b : trg_booking_audit_update
-- Audit uniquement si le statut a réellement changé
DELIMITER $$

CREATE TRIGGER trg_booking_audit_update
AFTER UPDATE ON booking
FOR EACH ROW
BEGIN
  IF OLD.payment_status != NEW.payment_status THEN
    INSERT INTO booking_audit (booking_id, workshop_id, member_id, action, old_status, new_status)
    VALUES (NEW.booking_id, NEW.workshop_id, NEW.member_id, 'UPDATE', OLD.payment_status, NEW.payment_status);
  END IF;
END$$

DELIMITER ;


-- ============================================================
-- FONCTIONS STOCKÉES
-- ============================================================

-- Fonction 1 : fn_nb_participants_actifs
-- Retourne le nombre de participants actifs (PENDING + PAID) d'un atelier
DELIMITER $$

CREATE FUNCTION fn_nb_participants_actifs(p_workshop_id INT)
RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
  DECLARE v_count INT;

  SELECT COUNT(*) INTO v_count
  FROM booking
  WHERE workshop_id = p_workshop_id
    AND payment_status != 'CANCELLED';

  RETURN v_count;
END$$

DELIMITER ;


-- Fonction 2 : fn_revenu_artiste
-- Calcule le chiffre d'affaires réalisé par un artiste (oeuvres SOLD)
DELIMITER $$

CREATE FUNCTION fn_revenu_artiste(p_artist_id INT)
RETURNS DECIMAL(15,2)
READS SQL DATA
DETERMINISTIC
BEGIN
  DECLARE v_total DECIMAL(15,2);

  SELECT COALESCE(SUM(price), 0.00) INTO v_total
  FROM artwork
  WHERE artist_id = p_artist_id
    AND status    = 'SOLD';

  RETURN v_total;
END$$

DELIMITER ;


-- ============================================================
-- PROCÉDURES STOCKÉES
-- ============================================================

-- Procédure 1 : sp_inscrire_membre
-- Inscrit un membre à un atelier de façon sécurisée
DELIMITER $$

CREATE PROCEDURE sp_inscrire_membre(
  IN  p_member_id   INT,
  IN  p_workshop_id INT,
  OUT p_resultat    VARCHAR(255)
)
BEGIN
  DECLARE v_exists  INT DEFAULT 0;
  DECLARE v_already INT DEFAULT 0;

  SELECT COUNT(*) INTO v_exists  FROM workshop WHERE workshop_id = p_workshop_id;
  SELECT COUNT(*) INTO v_already FROM booking
  WHERE workshop_id = p_workshop_id AND member_id = p_member_id AND payment_status != 'CANCELLED';

  IF v_exists = 0 THEN
    SET p_resultat = 'ERREUR : atelier introuvable.';
  ELSEIF v_already > 0 THEN
    SET p_resultat = 'ERREUR : le membre est déjà inscrit à cet atelier.';
  ELSE
    INSERT INTO booking (workshop_id, member_id, payment_status)
    VALUES (p_workshop_id, p_member_id, 'PENDING');
    SET p_resultat = CONCAT('OK : inscription créée (booking_id = ', LAST_INSERT_ID(), ').');
  END IF;
END$$

DELIMITER ;


-- Procédure 2 : sp_annuler_atelier
-- Annule toutes les réservations actives d'un atelier
DELIMITER $$

CREATE PROCEDURE sp_annuler_atelier(
  IN  p_workshop_id    INT,
  OUT p_nb_annulations INT
)
BEGIN
  UPDATE booking
  SET    payment_status = 'CANCELLED'
  WHERE  workshop_id    = p_workshop_id
    AND  payment_status != 'CANCELLED';

  SET p_nb_annulations = ROW_COUNT();
END$$

DELIMITER ;


-- Procédure 3 : sp_top_oeuvres_artiste
-- Affiche le classement des oeuvres d'un artiste par note moyenne décroissante
DELIMITER $$

CREATE PROCEDURE sp_top_oeuvres_artiste(
  IN p_artist_id INT,
  IN p_limit     INT
)
BEGIN
  SELECT
    a.artwork_id,
    a.title,
    a.type,
    a.price,
    a.status,
    ROUND(AVG(r.rating), 2) AS note_moyenne,
    COUNT(r.review_id)      AS nb_avis
  FROM artwork a
  LEFT JOIN review r ON a.artwork_id = r.artwork_id
  WHERE a.artist_id = p_artist_id
  GROUP BY a.artwork_id, a.title, a.type, a.price, a.status
  ORDER BY note_moyenne DESC, nb_avis DESC
  LIMIT p_limit;
END$$

DELIMITER ;


-- Procédure 4 : sp_creer_exposition
-- Crée une exposition dans une galerie et retourne son ID
DELIMITER $$

CREATE PROCEDURE sp_creer_exposition(
  IN  p_title        VARCHAR(255),
  IN  p_start_date   DATE,
  IN  p_end_date     DATE,
  IN  p_curator      VARCHAR(150),
  IN  p_theme        VARCHAR(150),
  IN  p_gallery_id   INT,
  OUT p_exhibition_id INT
)
BEGIN
  IF p_end_date < p_start_date THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ERREUR : la date de fin est antérieure à la date de début.';
  END IF;

  INSERT INTO exhibition (title, start_date, end_date, curator_name, theme, gallery_id)
  VALUES (p_title, p_start_date, p_end_date, p_curator, p_theme, p_gallery_id);

  SET p_exhibition_id = LAST_INSERT_ID();
END$$

DELIMITER ;


-- Procédure 5 : sp_inscription_multiple
-- Inscrit un membre à deux ateliers de façon atomique (ROLLBACK si erreur)
DELIMITER $$

CREATE PROCEDURE sp_inscription_multiple(
  IN  p_member_id   INT,
  IN  p_workshop_id1 INT,
  IN  p_workshop_id2 INT,
  OUT p_resultat    VARCHAR(255)
)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    SET p_resultat = 'ERREUR : inscription annulée (rollback effectué).';
  END;

  START TRANSACTION;

  INSERT INTO booking (workshop_id, member_id, payment_status)
  VALUES (p_workshop_id1, p_member_id, 'PENDING');

  INSERT INTO booking (workshop_id, member_id, payment_status)
  VALUES (p_workshop_id2, p_member_id, 'PENDING');

  COMMIT;
  SET p_resultat = CONCAT('OK : membre ', p_member_id,
                          ' inscrit aux ateliers ', p_workshop_id1,
                          ' et ', p_workshop_id2, '.');
END$$

DELIMITER ;
