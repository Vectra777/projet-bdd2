-- ============================================================
-- ArtConnect - Étape 3.1 : Données d'exemple
-- MySQL 8.0+  |  Cours TI603 – INGE1
-- ============================================================

USE artconnect_db;

-- --------------------------------------------------------
-- RÉFÉRENCES
-- --------------------------------------------------------

INSERT INTO discipline (name) VALUES
('Peinture'),
('Sculpture'),
('Photographie'),
('Dessin'),
('Art numérique'),
('Gravure'),
('Céramique');

INSERT INTO artwork_tag (name) VALUES
('Contemporain'),
('Abstrait'),
('Figuratif'),
('Minimaliste'),
('Expressionniste'),
('Urbain'),
('Nature');

-- --------------------------------------------------------
-- ARTISTES
-- --------------------------------------------------------

INSERT INTO artist (name, bio, birth_year, contact_email, phone, city, website, social_media, is_active) VALUES
('Sophie Marchand',
 'Peintre contemporaine spécialisée en abstraction lyrique. Formée aux Beaux-Arts de Paris.',
 1985, 'sophie.marchand@artconnect.fr', '0612345678', 'Paris',
 'www.sophiemarchand.fr', '@sophiemarchand_art', TRUE),

('Thomas Renard',
 'Sculpteur plasticien travaillant le métal et le bois récupéré. Prix de la jeune création 2018.',
 1979, 'thomas.renard@artconnect.fr', '0623456789', 'Lyon',
 'www.thomasrenard.fr', '@thomas_sculpture', TRUE),

('Camille Dubois',
 'Photographe documentaire, spécialiste des espaces urbains abandonnés.',
 1992, 'camille.dubois@artconnect.fr', '0634567890', 'Bordeaux',
 NULL, '@camille_photo', TRUE),

('Lucas Martin',
 'Artiste numérique et illustrateur, entre esthétique glitch et géométrie fractale.',
 1995, 'lucas.martin@artconnect.fr', '0645678901', 'Nantes',
 'www.lucasmartin.art', '@lucas_pixel', TRUE),

('Élise Bernard',
 'Céramiste et graveuse, ateliers réguliers à Paris. Expose en France et en Belgique.',
 1988, 'elise.bernard@artconnect.fr', '0656789012', 'Paris',
 NULL, '@elise_ceramique', TRUE);

-- --------------------------------------------------------
-- OEUVRES
-- --------------------------------------------------------

INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
-- Statuts initiaux cohérents avec exhibition_artwork :
-- artwork 1,2,3,6,7,8 → EXHIBITED  (présents dans une exposition)
-- artwork 4,5          → FOR_SALE / statut libre (non exposés)
-- artwork 9            → EXHIBITED  (dans Collection Printemps)
-- artwork 10           → EXHIBITED  (dans Collection Printemps ; vendu mais toujours exposé)
('Lumière Bleue',    2021, 'Peinture',      'Huile sur toile',         '80x100 cm',  'Jeu de lumières abstraites en camaïeu bleu.',                          1200.00, 'EXHIBITED', 1),
('Vague d''Été',     2022, 'Peinture',      'Acrylique sur toile',     '60x80 cm',   'Évocation marine et estivale, tons chauds.',                            950.00, 'EXHIBITED', 1),
('Torsion #3',       2020, 'Sculpture',     'Acier soudé poli',        '40x40x80 cm','Forme torsadée en acier inox, finition miroir.',                       3200.00, 'EXHIBITED', 2),
('Racines',          2019, 'Sculpture',     'Bois et résine',          '30x30x60 cm','Enchevêtrement de racines naturelles coulées en résine.',               1800.00, 'SOLD',      2),
('Rue de Rivoli #12',2023, 'Photographie',  'Tirage argentique',       '50x70 cm',   'Paris sous la pluie, soir d''hiver, longue exposition.',                 600.00, 'FOR_SALE',  3),
('Métro Fantôme',    2022, 'Photographie',  'Tirage numérique',        '40x60 cm',   'Station de métro désaffectée, lumière rasante.',                         450.00, 'EXHIBITED', 3),
('Pixel Dreams',     2023, 'Art numérique', 'Impression sur aluminium','70x70 cm',   'Composition fractale générée par algorithme bruité.',                    750.00, 'EXHIBITED', 4),
('Glitch City',      2023, 'Art numérique', 'Impression + tirage NFT', '50x50 cm',   'Esthétique glitch appliquée à un panorama urbain.',                      500.00, 'EXHIBITED', 4),
('Bol Céladon',      2021, 'Céramique',     'Grès émaillé tourné main','Ø 18 cm',    'Bol à thé, email céladon mat, cuisson four à bois.',                     320.00, 'EXHIBITED', 5),
('Gravure Forestière',2020,'Gravure',       'Eau-forte sur papier',    '30x40 cm',   'Sous-bois mystérieux en noir et blanc, tirage 1/30.',                    280.00, 'EXHIBITED', 5);

-- --------------------------------------------------------
-- GALERIES
-- --------------------------------------------------------

INSERT INTO gallery (name, address, owner_name, opening_hours, contact_phone, rating, website) VALUES
('Galerie Lumière',        '12 rue du Faubourg Saint-Antoine, 75011 Paris', 'Anne Lefebvre', 'Mar-Sam 10h-19h', '0142345678', 4.5, 'www.galerielumiere.fr'),
('Espace Art Contemporain','8 place Bellecour, 69002 Lyon',                 'Marc Girard',   'Lun-Ven 9h-18h',  '0478234567', 4.2, 'www.espaceac-lyon.fr'),
('Studio Photon',          '3 allée des Arts, 33000 Bordeaux',             'Julia Petit',   'Mer-Dim 11h-20h', '0556789012', 3.9, NULL);

-- --------------------------------------------------------
-- EXPOSITIONS
-- --------------------------------------------------------

INSERT INTO exhibition (title, start_date, end_date, description, curator_name, theme, gallery_id) VALUES
('Lumières de Paris',    '2024-02-01', '2024-03-15', 'Exposition collective autour des jeux de lumière et de couleur.',            'Anne Lefebvre', 'Lumière & Couleur',  1),
('Formes et Matières',   '2024-04-10', '2024-05-25', 'Sculptures et installations questionnant la matérialité des objets.',        'Marc Girard',   'Matérialité',        2),
('Urbain Poétique',      '2024-06-01', '2024-07-31', 'Photographies et art numérique explorant la poésie des espaces urbains.',    'Julia Petit',   'Urbanité',           3),
('Collection Printemps', '2024-03-20', '2024-04-05', 'Sélection de nouvelles acquisitions et découvertes de la saison.',           'Anne Lefebvre', 'Découverte',         1);

-- --------------------------------------------------------
-- ATELIERS
-- --------------------------------------------------------

INSERT INTO workshop (title, date, duration_minutes, max_participants, price, location, description, level, instructor_id) VALUES
('Initiation à l''aquarelle',    '2024-03-10 10:00:00', 180, 12, 45.00, 'Galerie Lumière, Paris',    'Découverte des techniques aquarelle avec Sophie Marchand.',                 'beginner',     1),
('Sculpture métal avancée',       '2024-04-20 14:00:00', 240,  6, 80.00, 'Atelier Thomas Renard, Lyon','Techniques de soudure TIG et finition sur acier.',                          'advanced',     2),
('Photo de rue',                  '2024-05-05 09:00:00', 300, 10, 60.00, 'Bordeaux centre-ville',     'Sortie photo commentée : composition, lumière, mise en scène.',             'intermediate', 3),
('Art numérique pour débutants',  '2024-06-15 14:00:00', 120, 15, 35.00, 'En ligne (Zoom)',           'Premiers pas avec Procreate et les outils de création numérique.',          'beginner',     4);

-- --------------------------------------------------------
-- MEMBRES
-- --------------------------------------------------------

INSERT INTO community_member (name, email, birth_year, phone, city, membership_type) VALUES
('Alice Dupont',   'alice.dupont@mail.fr',    1990, '0611111111', 'Paris',    'premium'),
('Bob Legrand',    'bob.legrand@mail.fr',     1985, '0622222222', 'Lyon',     'free'),
('Clara Petit',    'clara.petit@mail.fr',     1998, '0633333333', 'Bordeaux', 'premium'),
('David Morel',    'david.morel@mail.fr',     1992, '0644444444', 'Nantes',   'free'),
('Emma Rousseau',  'emma.rousseau@mail.fr',   2000, '0655555555', 'Paris',    'premium'),
('François Blanc', 'francois.blanc@mail.fr',  1988, '0666666666', 'Lille',    'free');

-- --------------------------------------------------------
-- RÉSERVATIONS
-- --------------------------------------------------------

INSERT INTO booking (workshop_id, member_id, booking_date, payment_status) VALUES
-- Atelier aquarelle : 3 inscrits dont un PENDING
(1, 1, '2024-02-15 10:00:00', 'PAID'),
(1, 2, '2024-02-16 11:00:00', 'PAID'),
(1, 5, '2024-02-17 09:00:00', 'PENDING'),
-- Atelier sculpture : 1 payé, 1 annulé
(2, 3, '2024-03-01 14:00:00', 'PAID'),
(2, 4, '2024-03-02 15:00:00', 'CANCELLED'),
-- Photo de rue : Alice et François
(3, 1, '2024-04-01 09:00:00', 'PAID'),
(3, 6, '2024-04-02 10:00:00', 'PENDING'),
-- Art numérique : Bob et Emma
(4, 2, '2024-05-10 11:00:00', 'PAID'),
(4, 5, '2024-05-11 12:00:00', 'PAID');

-- --------------------------------------------------------
-- AVIS
-- --------------------------------------------------------

INSERT INTO review (member_id, artwork_id, rating, comment, review_date) VALUES
(1, 1, 5, 'Magnifique jeu de couleurs, très émouvant.',         '2024-02-20'),
(1, 5, 4, 'Très belle photo, composition maîtrisée.',           '2024-03-01'),
(2, 1, 4, 'Belle pièce, un peu chère à mon goût.',              '2024-02-22'),
(2, 7, 5, 'L''art numérique à son meilleur, bluffant.',          '2024-03-15'),
(3, 3, 5, 'Sculpture impressionnante, technique irréprochable.', '2024-03-20'),
(3, 6, 4, 'Atmosphère saisissante, belle lumière.',              '2024-04-01'),
(4, 2, 3, 'Joli mais pas exceptionnel selon moi.',               '2024-03-10'),
(5, 8, 5, 'Glitch City est vraiment originale, coup de coeur !', '2024-04-05'),
(6, 9, 4, 'Beau travail de céramique, finesse remarquable.',     '2024-04-10');

-- --------------------------------------------------------
-- TABLES DE JOINTURE
-- --------------------------------------------------------

-- Artiste -> Discipline
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
(1, 1), (1, 4),   -- Sophie : Peinture, Dessin
(2, 2),           -- Thomas : Sculpture
(3, 3),           -- Camille : Photographie
(4, 5), (4, 4),   -- Lucas : Art numérique, Dessin
(5, 7), (5, 6);   -- Élise : Céramique, Gravure

-- Oeuvre -> Tag
INSERT INTO artwork_tag_map (artwork_id, tag_id) VALUES
(1, 1), (1, 2),   -- Lumière Bleue : Contemporain, Abstrait
(2, 1), (2, 3),   -- Vague d'Été : Contemporain, Figuratif
(3, 1), (3, 4),   -- Torsion #3 : Contemporain, Minimaliste
(4, 3), (4, 7),   -- Racines : Figuratif, Nature
(5, 6), (5, 3),   -- Rue de Rivoli : Urbain, Figuratif
(6, 6), (6, 4),   -- Métro Fantôme : Urbain, Minimaliste
(7, 1), (7, 2),   -- Pixel Dreams : Contemporain, Abstrait
(8, 6), (8, 1);   -- Glitch City : Urbain, Contemporain

-- Exposition -> Oeuvre
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
(1, 1), (1, 2),           -- Lumières de Paris
(2, 3),                   -- Formes et Matières
(3, 6), (3, 7), (3, 8),   -- Urbain Poétique
(4, 9), (4, 10);          -- Collection Printemps

-- Membre -> Discipline (favoris)
INSERT INTO member_discipline (member_id, discipline_id) VALUES
(1, 1), (1, 3),   -- Alice : Peinture, Photographie
(2, 2),           -- Bob : Sculpture
(3, 3), (3, 5),   -- Clara : Photographie, Art numérique
(4, 5),           -- David : Art numérique
(5, 1), (5, 2),   -- Emma : Peinture, Sculpture
(6, 6);           -- François : Gravure
