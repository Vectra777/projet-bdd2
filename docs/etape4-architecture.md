# Architecture Etape 4

L'application suit une architecture en couches simple et conforme au squelette fourni :

- `ui` : contrôleurs JavaFX et vues FXML.
- `service` : logique métier consommée par l'UI.
- `dao` : contrats d'accès aux données.
- `persistence` : implémentations JDBC connectées à `artconnect_db`.
- `model` : entités métier sans identifiants techniques visibles.

Flux principal :

`UI -> Service -> DAO -> JDBC -> MySQL`

Choix retenus pour rester alignés avec votre MLD :

- Les clés techniques (`artist_id`, `artwork_id`, etc.) restent côté base uniquement.
- Les relations N-N (`artist_discipline`, `artwork_tag_map`, `exhibition_artwork`, `member_discipline`) sont reconstruites en objets Java lors du chargement.
- `ServiceProvider` essaie d'initialiser les services JDBC ; en cas d'échec de connexion, il bascule sur les services `InMemory` pour conserver une application exécutable.
