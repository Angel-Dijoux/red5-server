> **Note**  
> Les modifications ont été apportées dans l'ensemble du projet red-5-server, même si nous avions fait notre analyse dans `common` pour la partie 1. En effet, à force de modifier le code, tester et naviguer, j'ai trouvé plein de choses à modifier.

## 1. Petites Modifications

### 1.1. Réorganisation des attributs et méthodes

- **Commit :** [e675387ad9c62d6f97274b879414954d2cf5e7d9](https://github.com/Angel-Dijoux/red5-server/commit/e675387ad9c62d6f97274b879414954d2cf5e7d9)
- **Situation existante :** La disposition des variables d’instance et des méthodes (publiques puis privées) n’était pas uniforme dans certaines classes.
- **Modification apportée :** Réorganisation de l’ordre des attributs et des méthodes pour respecter un ordre logique (attributs en début de classe, suivis des méthodes publiques et ensuite privées).
- **Amélioration :** Cette structure facilite la lecture, la maintenance et la compréhension du code par d’autres développeurs.

---

### 1.2. Correction et ajout de tests pour ConversionUtils

- **Commit :** [69d3d57186e0c2e801d5312d09ab51dd9b772e96](https://github.com/Angel-Dijoux/red5-server/commit/69d3d57186e0c2e801d5312d09ab51dd9b772e96)
- **Situation existante :** Un test lié à la conversion dans ConversionUtils posait problème, indiquant un dysfonctionnement ou un comportement inattendu.
- **Modification apportée :** Résolution du test de conversion et ajustement du code pour que ConversionUtils se comporte comme attendu.
- **Amélioration :** La correction des tests garantit une meilleure fiabilité du module et facilite les évolutions futures en réduisant le risque de régression.

---

### 1.3. Renommage de l’énumération INPUT_TYPE

- **Commit :** [9490f328a79b17da25c85a8f0156e8ed792e67ec](https://github.com/Angel-Dijoux/red5-server/commit/9490f328a79b17da25c85a8f0156e8ed792e67ec)
- **Situation existante :** L’énumération s’appelait `INPUT_TYPE`, ce qui ne respectait pas les conventions de nommage Java.
- **Modification apportée :** Renommage en `InputKind` afin de suivre les conventions (camelCase pour les noms d’énumérations).
- **Amélioration :** L’uniformisation du code selon les conventions améliore sa lisibilité et sa maintenabilité.

---

### 1.4. Suppression du code mort et des variables inutilisées

**Note**

> IIl y a eu une tentative présente de suppression de code mort dans la classe `PlaylistSubscriberStream` avec la méthode `getCurrentTimestamp`, qui ne semblait pas être appelée. Mais c'était en fait lié à l'abstraction mise en place, j'ai donc dû annuler le changement.
> ![[Pasted image 20250406205228.png]]

- **Commit :** [e69e5d31851e6f5cfa0acf6510635f0c187718f0](https://github.com/Angel-Dijoux/red5-server/commit/e69e5d31851e6f5cfa0acf6510635f0c187718f0)
- **Situation existante :** Présence de variable non-utilisé.
- **Modification apportée :** Suppression du code mort pour nettoyer la base de code.
- **Amélioration :** Réduction du « bruit » dans le code, ce qui permet de faciliter la compréhension et d’éviter les potentiels effets de bord lors de modifications futures.

---

### 1.5. Suppression d’un nombre magique

- **Commit :** [895e2fb85d53ab2cbe8e9efe66ea617a42789ac7](https://github.com/Angel-Dijoux/red5-server/commit/895e2fb85d53ab2cbe8e9efe66ea617a42789ac7)
- **Situation existante :** Utilisation de valeurs numériques codées en dur dans le code.
- **Modification apportée :** Remplacement du nombre magique par une variable ou une constante significative.
- **Amélioration :** Le code devient plus lisible et plus facilement modifiable, car la signification de la valeur est explicitée et centralisée.

---

### 1.6. Tests complémentaires sur RTMPUtils

- **Commit :** [4fc2b77d6747a41c295cb84a97195ae5adbd6b8a](https://github.com/Angel-Dijoux/red5-server/commit/4fc2b77d6747a41c295cb84a97195ae5adbd6b8a)
- **Situation existante :** Nécessité de vérifier le bon fonctionnement des utilitaires liés au protocole RTMP.
- **Modification apportée :** Ajout des tests pour la classe RTMPUtils.
- **Amélioration :** Une meilleure couverture de test permet d’assurer la stabilité des fonctionnalités liées à RTMP et d’identifier rapidement d’éventuelles régressions.

---

## 2. Modifications Moyennes

### 2.1. Réduction de la complexité cyclomatique dans Aggregate.java

- **Commit :** [10a2b4f326290a0e3f37e27a3eced17274f20050](https://github.com/Angel-Dijoux/red5-server/commit/10a2b4f326290a0e3f37e27a3eced17274f20050)
- **Situation existante :** La méthode `getPart` de la classe `Aggregate.java` était trop complexe (forte complexité cyclomatique), rendant le code difficile à comprendre et à maintenir.
- **Modification apportée :** Refactorisation de la méthode afin de réduire sa complexité cyclomatique.
- **Amélioration :** La simplification du flux de contrôle améliore la lisibilité et facilite la maintenance et l’évolution de cette partie du système.

---

### 2.2. Généralisation de la gestion des événements multimédias

- **Commit :** [180cecd62ac30c9411538e22ce777894350b0bcf](https://github.com/Angel-Dijoux/red5-server/commit/180cecd62ac30c9411538e22ce777894350b0bcf)
- **Situation existante :** Les événements liés aux données audio, vidéo et aux agrégats comportaient une logique commune, dupliquée dans plusieurs classes.
- **Modification apportée :** Création d’une classe abstraite `MediaDataStreamEvent` pour centraliser la logique commune entre `videoData`, `audioData` et `Aggregate`.
- **Amélioration :** La généralisation permet de réduire la duplication du code, de faciliter l’extension future du système et de garantir une cohérence dans le traitement des différents types d’événements multimédias.

---

### 2.3. Remplacement du retour d’un code d’erreur par l’utilisation de Optional

- **Commit :** [ea8ad727412d3d949944eb6b4ed6ffafe53c01f1](https://github.com/Angel-Dijoux/red5-server/commit/ea8ad727412d3d949944eb6b4ed6ffafe53c01f1)
- **Situation existante :** La méthode `sendVODSeekCM` retournait un code d’erreur (`-1`) en cas d’échec, nécessitant une gestion étrange de l'erreur.
- **Modification apportée :** Remplacement du code d’erreur par l’utilisation de `Optional`, offrant une approche plus fonctionnelle et claire.
- **Amélioration :** Cette modification simplifie la gestion des erreurs et réduit la complexité du code appelant, tout en rendant le comportement de la méthode plus explicite.

---

### 2.4. Transformation d’une classe statique en classe instanciable

- **Commit :** [6899203d329c07a21924c724a2e9bd20a13a1f69](https://github.com/Angel-Dijoux/red5-server/commit/6899203d329c07a21924c724a2e9bd20a13a1f69)
- **Situation existante :** `ArrayUtils` était implémentée comme une classe statique, limitant la flexibilité en cas de besoin d’extension ou de personnalisation.
- **Modification apportée :** Conversion de la classe statique en classe instanciable.
- **Amélioration :** La nouvelle structure permet d’hériter et de spécialiser le comportement si nécessaire, offrant ainsi plus de souplesse et facilitant l’intégration dans des contextes variés.

---

### 2.5. Initialisation d’un check CI simple

> **Note**  
> La CI n'est pas complète et demanderait plus de temps pour être configurée entièrement pour le projet red-5-server, notamment avec un système de release propre, des vérifications des commits, un rapport PMD pour les pull requests, etc. Cela requiert un temps que je n'ai pas eu, mais voilà un premier jet.

- **Commit :** [cf4f857040ab798d40d6861d22b860912168f92f](https://github.com/Angel-Dijoux/red5-server/commit/cf4f857040ab798d40d6861d22b860912168f92f)
- **Situation existante :** Avant cette modification, il n’existait pas de vérification continue automatisée des commits.
- **Modification apportée :** Mise en place d’un check CI simple pour automatiser certaines vérifications de base.
- **Amélioration :**
  - Assure une première validation automatisée des commits, réduisant le risque d’introduction d’erreurs et améliorant la qualité du code au fil des évolutions.

---

## 3. Grandes Modifications

### 3.1. Refactorisation approfondie de la classe MP4Reader

**Note**

> Cette classe n'était pas présente dans notre analyse de la partie 1, mais elle corrobore les conclusions de celle-ci. Elle semblait intéressante à refactoriser, notamment en raison de son interaction avec le format MP4. J'ai essayé de découper les différents commits au mieux, mais ils restent malgré tout volumineux en raison de la nature du travail auquel je me suis attelée.

- **Commit principal :** [286b8e61ee3de76bd3e9bae63884d8a894970dea](https://github.com/Angel-Dijoux/red5-server/commit/286b8e61ee3de76bd3e9bae63884d8a894970dea)
- **Autres commits associés :**
  - [cf76efc383bd298b9065b873d0fa3c22c30ab844](https://github.com/Angel-Dijoux/red5-server/commit/cf76efc383bd298b9065b873d0fa3c22c30ab844) – Création de tests pour la refactorisation
  - [d93fd1bc7a58f837253d059cdf8e74c718123e0b](https://github.com/Angel-Dijoux/red5-server/commit/d93fd1bc7a58f837253d059cdf8e74c718123e0b) – Suite de la refactorisation
- **Situation existante :** La classe `MP4Reader` était surchargée, regroupant de nombreuses responsabilités, ce qui en faisait une « god classe ».
- **Modification apportée :**
  - Découpage de `MP4Reader` en plusieurs classes spécialisées :
    - `MP4Parser` pour l’analyse syntaxique des films MP4
    - `MP4TrackInfo` pour encapsuler les métadonnées d’un fichier MP4
    - `MP4FileMetadata` qui s’appuie sur `MP4TrackInfo`
    - `MP4FrameAnalyser` pour analyser les frames vidéo et les pistes audio associées
    - Une classe dédiée pour l’analyse des entrées MP4
  - Ajout de tests pour vérifier que la nouvelle architecture respecte les fonctionnalités attendues.
- **Amélioration :**
  - La décomposition de la classe améliore la séparation des responsabilités, réduit la complexité et facilite la maintenance.
  - Chaque classe spécialisée est plus simple à comprendre, tester et faire évoluer indépendamment.
  - L’ajout de tests garantit que la refactorisation n’introduit pas de régressions dans le système.

---

### 3.2. Application du design pattern Strategy pour WebmWriter

- **Commit :** [da10afc66ec88efee1294bc4f4ef200cd036c2d9](https://github.com/Angel-Dijoux/red5-server/commit/da10afc66ec88efee1294bc4f4ef200cd036c2d9)
- **Situation existante :** La classe WebmWriter présentait une logique rigide et difficile à étendre ou modifier.
- **Modification apportée :** Mise en place du design pattern Strategy, permettant de déléguer certaines responsabilités à des classes spécifiques et de rendre la classe plus flexible.
- **Amélioration :** Cette approche favorise la réutilisation du code et facilite l’ajout de nouvelles stratégies sans impacter la structure de base, ce qui améliore l’évolutivité du module.
