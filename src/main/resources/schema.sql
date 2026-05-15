-- ============================================================
-- ArtConnect Pro - Schéma de base de données
-- MySQL 8.0+  |  Cours TI603 – INGE1
-- ============================================================

CREATE DATABASE IF NOT EXISTS artconnect_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE artconnect_db;

-- ============================================================
-- TABLES DE RÉFÉRENCE
-- ============================================================

CREATE TABLE IF NOT EXISTS discipline (
  discipline_id INT           AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100)  NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS artwork_tag (
  tag_id  INT          AUTO_INCREMENT PRIMARY KEY,
  name    VARCHAR(100) NOT NULL UNIQUE
);

-- ============================================================
-- ARTISTE
-- ============================================================

CREATE TABLE IF NOT EXISTS artist (
  artist_id     INT          AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(150) NOT NULL,
  bio           TEXT,
  birth_year    SMALLINT,
  contact_email VARCHAR(255) UNIQUE,
  phone         VARCHAR(30),
  city          VARCHAR(100),
  website       VARCHAR(255),
  social_media  VARCHAR(255),
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ============================================================
-- OEUVRE
-- ============================================================

CREATE TABLE IF NOT EXISTS artwork (
  artwork_id     INT            AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(255)   NOT NULL,
  creation_year  SMALLINT,
  type           VARCHAR(100),
  medium         VARCHAR(100),
  dimensions     VARCHAR(100),
  description    TEXT,
  price          DECIMAL(15,2),
  status         ENUM('FOR_SALE','SOLD','EXHIBITED') NOT NULL DEFAULT 'FOR_SALE',
  artist_id      INT            NOT NULL,
  CONSTRAINT fk_artwork_artist
    FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================
-- GALERIE
-- ============================================================

CREATE TABLE IF NOT EXISTS gallery (
  gallery_id     INT           AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(150)  NOT NULL,
  address        VARCHAR(255),
  owner_name     VARCHAR(150),
  opening_hours  VARCHAR(100),
  contact_phone  VARCHAR(30),
  rating         DECIMAL(2,1),
  website        VARCHAR(255),
  CONSTRAINT chk_gallery_rating CHECK (rating BETWEEN 0.0 AND 5.0)
);

-- ============================================================
-- EXPOSITION
-- ============================================================

CREATE TABLE IF NOT EXISTS exhibition (
  exhibition_id  INT          AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(255) NOT NULL,
  start_date     DATE,
  end_date       DATE,
  description    TEXT,
  curator_name   VARCHAR(150),
  theme          VARCHAR(150),
  gallery_id     INT          NOT NULL,
  CONSTRAINT fk_exhibition_gallery
    FOREIGN KEY (gallery_id) REFERENCES gallery(gallery_id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_exhibition_dates
    CHECK (end_date >= start_date)
);

-- ============================================================
-- ATELIER
-- ============================================================

CREATE TABLE IF NOT EXISTS workshop (
  workshop_id       INT           AUTO_INCREMENT PRIMARY KEY,
  title             VARCHAR(255)  NOT NULL,
  date              DATETIME,
  duration_minutes  INT,
  max_participants  INT,
  price             DECIMAL(10,2),
  location          VARCHAR(255),
  description       TEXT,
  level             ENUM('beginner','intermediate','advanced'),
  instructor_id     INT           NOT NULL,
  CONSTRAINT fk_workshop_instructor
    FOREIGN KEY (instructor_id) REFERENCES artist(artist_id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_workshop_max
    CHECK (max_participants > 0)
);

-- ============================================================
-- MEMBRE DE LA COMMUNAUTÉ
-- ============================================================

CREATE TABLE IF NOT EXISTS community_member (
  member_id        INT          AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(150) NOT NULL,
  email            VARCHAR(255) NOT NULL UNIQUE,
  birth_year       SMALLINT,
  phone            VARCHAR(30),
  city             VARCHAR(100),
  membership_type  ENUM('free','premium') NOT NULL DEFAULT 'free'
);

-- ============================================================
-- RÉSERVATION (entité associative Booking)
-- ============================================================

CREATE TABLE IF NOT EXISTS booking (
  booking_id      INT       AUTO_INCREMENT PRIMARY KEY,
  workshop_id     INT       NOT NULL,
  member_id       INT       NOT NULL,
  booking_date    DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  payment_status  ENUM('PENDING','PAID','CANCELLED') NOT NULL DEFAULT 'PENDING',
  CONSTRAINT fk_booking_workshop
    FOREIGN KEY (workshop_id) REFERENCES workshop(workshop_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_booking_member
    FOREIGN KEY (member_id) REFERENCES community_member(member_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT uq_booking
    UNIQUE (workshop_id, member_id)
);

-- ============================================================
-- AVIS (entité associative Review)
-- ============================================================

CREATE TABLE IF NOT EXISTS review (
  review_id    INT      AUTO_INCREMENT PRIMARY KEY,
  member_id    INT      NOT NULL,
  artwork_id   INT      NOT NULL,
  rating       TINYINT  NOT NULL,
  comment      TEXT,
  review_date  DATE     NOT NULL DEFAULT (CURDATE()),
  CONSTRAINT fk_review_member
    FOREIGN KEY (member_id) REFERENCES community_member(member_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_review_artwork
    FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_review_rating
    CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT uq_review
    UNIQUE (member_id, artwork_id)
);

-- ============================================================
-- TABLES DE JOINTURE N-N
-- ============================================================

-- Artiste -> Discipline
CREATE TABLE IF NOT EXISTS artist_discipline (
  artist_id     INT NOT NULL,
  discipline_id INT NOT NULL,
  PRIMARY KEY (artist_id, discipline_id),
  CONSTRAINT fk_ad_artist
    FOREIGN KEY (artist_id) REFERENCES artist(artist_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ad_discipline
    FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- Oeuvre -> Tag
CREATE TABLE IF NOT EXISTS artwork_tag_map (
  artwork_id INT NOT NULL,
  tag_id     INT NOT NULL,
  PRIMARY KEY (artwork_id, tag_id),
  CONSTRAINT fk_atm_artwork
    FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_atm_tag
    FOREIGN KEY (tag_id) REFERENCES artwork_tag(tag_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- Exposition -> Oeuvre
CREATE TABLE IF NOT EXISTS exhibition_artwork (
  exhibition_id INT NOT NULL,
  artwork_id    INT NOT NULL,
  PRIMARY KEY (exhibition_id, artwork_id),
  CONSTRAINT fk_ea_exhibition
    FOREIGN KEY (exhibition_id) REFERENCES exhibition(exhibition_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ea_artwork
    FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Membre -> Discipline (favoris)
CREATE TABLE IF NOT EXISTS member_discipline (
  member_id     INT NOT NULL,
  discipline_id INT NOT NULL,
  PRIMARY KEY (member_id, discipline_id),
  CONSTRAINT fk_md_member
    FOREIGN KEY (member_id) REFERENCES community_member(member_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_md_discipline
    FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================
-- TABLE D'AUDIT (nécessaire pour trigger 3)
-- ============================================================

CREATE TABLE IF NOT EXISTS booking_audit (
  audit_id    INT AUTO_INCREMENT PRIMARY KEY,
  booking_id  INT,
  workshop_id INT,
  member_id   INT,
  action      ENUM('INSERT','UPDATE','DELETE') NOT NULL,
  old_status  VARCHAR(20),
  new_status  VARCHAR(20),
  changed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
