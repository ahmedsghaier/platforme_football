import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, KFold
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_squared_error, r2_score
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout
from tensorflow.keras.optimizers import Adam
import logging
import psycopg2
from psycopg2 import Error
import warnings
import math

# Supprimer les warnings TensorFlow
warnings.filterwarnings('ignore')

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Database configuration
DB_CONFIG = {
    'host': 'localhost',
    'database': 'football_db',
    'user': 'postgres',
    'password': 'adminadmin',
    'port': 5432
}

class PlayerValuePredictor:
    def __init__(self):
        self.connection = None
        self.cursor = None
        self.scaler = StandardScaler()
        self.model = None
        self.feature_columns = None
        self.mean_mae = 0  # Initialize mean_mae as class variable

    def _connect_to_db(self):
        """Établit une connexion à la base de données"""
        try:
            if self.connection is None or self.connection.closed:
                self.connection = psycopg2.connect(**DB_CONFIG)
                self.cursor = self.connection.cursor()
                logger.info("Connexion à la base de données réussie")
        except Error as e:
            logger.error(f"Erreur de connexion à la base de données: {e}")
            raise

    def _close_db_connection(self):
        """Ferme la connexion à la base de données"""
        try:
            if self.cursor:
                self.cursor.close()
                self.cursor = None
            if self.connection and not self.connection.closed:
                self.connection.close()
                self.connection = None
                logger.info("Connexion à la base de données fermée")
        except Error as e:
            logger.error(f"Erreur lors de la fermeture de la connexion: {e}")

    def get_all_players_data(self):
        """Récupère toutes les données des joueurs avec leurs statistiques"""
        try:
            self._connect_to_db()
            query = """
            SELECT 
                p.id,
                p.name,
                p.age,
                p.position,
                p.nationality,
                p.club_id,
                c.name as club_name,
                c.league,
                p.created_at,
                -- Statistiques defensives
                COALESCE(d.tacles_reussis, 0) as tacles_reussis,
                COALESCE(d.interceptions, 0) as interceptions,
                COALESCE(d.duels_aeriens, 0) as duels_aeriens,
                COALESCE(d.duels_gagnes, 0) as duels_gagnes,
                COALESCE(d.clean_sheets, 0) as clean_sheets,
                COALESCE(d.cartons_jaunes, 0) as cartons_jaunes,
                COALESCE(d.cartons_rouge, 0) as cartons_rouge,
                COALESCE(d.minutes_jouees, 0) as def_minutes,
                -- Statistiques gardiens
                COALESCE(g.pourcentage_arrets, 0) as pourcentage_arrets,
                COALESCE(g.clean_sheets, 0) as gk_clean_sheets,
                COALESCE(g.sorties, 0) as sorties,
                COALESCE(g.penalties_arretes, 0) as penalties_arretes,
                COALESCE(g.matchs_joues, 0) as gk_matchs,
                COALESCE(g.saves, 0) as saves,
                -- Statistiques milieux
                COALESCE(m.passes_reussies, 0) as passes_reussies,
                COALESCE(m.recuperations, 0) as recuperations,
                COALESCE(m.distance_parcourue, 0) as distance_parcourue,
                COALESCE(m.passes_cle, 0) as passes_cle,
                COALESCE(m.passes_decisives, 0) as passes_decisives,
                COALESCE(m.buts_marques, 0) as mid_buts,
                COALESCE(m.minutes_jouees, 0) as mid_minutes,
                -- Statistiques attaquants
                COALESCE(a.buts_marques, 0) as att_buts,
                COALESCE(a.taux_conversion, 0) as taux_conversion,
                COALESCE(a.passes_decisives, 0) as att_passes,
                COALESCE(a.minutes_jouees, 0) as att_minutes,
                COALESCE(a.shots_total, 0) as shots_total
            FROM public.players p
            LEFT JOIN public.clubs c ON p.club_id = c.id
            LEFT JOIN public.defenseurs d ON p.id = d.joueur_id
            LEFT JOIN public.gardiens g ON p.id = g.joueur_id
            LEFT JOIN public.milieux m ON p.id = m.joueur_id
            LEFT JOIN public.attaquants a ON p.id = a.joueur_id
            WHERE p.age IS NOT NULL AND p.position IS NOT NULL
            ORDER BY p.id ASC;
            """
            
            self.cursor.execute(query)
            columns = [desc[0] for desc in self.cursor.description]
            data = self.cursor.fetchall()
            df = pd.DataFrame(data, columns=columns)
            
            if df.empty:
                logger.warning("Aucune donnée récupérée de la base de données")
                return None
                
            logger.info(f"Récupération de {len(df)} joueurs avec leurs statistiques")
            return df
            
        except Error as e:
            logger.error(f"Erreur lors de la récupération des données: {e}")
            return None
        finally:
            self._close_db_connection()

    def calculate_base_value_by_position(self, row):
        """Calcule la valeur de base selon la position avec un bonus pour les minutes jouées"""
        try:
            position = row.get('position', 'MF')
            age = row.get('age', 25)
            league = row.get('league', 'Unknown')
            years_of_experience = max(age - 16, 0)  # Approximation simple
            
            base_values = {
                'GK': 15_000_000,
                'DF': 25_000_000,
                'MF': 30_000_000,
                'FW': 40_000_000,
                'MFDF': 28_000_000
            }
            
            league_multipliers = {
                'Premier League': 1.5,
                'La Liga': 1.4,
                'Serie A': 1.3,
                'Bundesliga': 1.3,
                'Ligue 1': 1.2,
                'Eredivisie': 0.8,
                'Primeira Liga': 0.7
            }
            
            # Facteur d'âge
            if age <= 21:
                age_multiplier = 1.3
            elif age <= 25:
                age_multiplier = 1.2
            elif age <= 28:
                age_multiplier = 1.0
            elif age <= 31:
                age_multiplier = 0.8
            else:
                age_multiplier = 0.6
            
            # Bonus d'expérience
            experience_bonus = min(years_of_experience * 500_000, 5_000_000)
            
            # Calcul des minutes jouées
            minutes = max(
                row.get('def_minutes', 0),
                row.get('mid_minutes', 0),
                row.get('att_minutes', 0),
                row.get('gk_matchs', 0) * 90
            )
            
            if minutes > 0:
                minutes_bonus = 10_000_000 * np.log1p(minutes / 1000)
                minutes_bonus = min(minutes_bonus, 20_000_000)
            else:
                minutes_bonus = 0
            
            base_value = base_values.get(position, 20_000_000)
            league_mult = league_multipliers.get(league, 1.0)
            
            return (base_value + minutes_bonus + experience_bonus) * league_mult * age_multiplier
            
        except Exception as e:
            logger.error(f"Erreur calcul valeur de base: {e}")
            return 20_000_000

    def calculate_performance_bonus(self, row):
        """Calcule le bonus de performance selon les statistiques"""
        try:
            position = row.get('position', 'MF')
            bonus = 0
            
            if position == 'GK':
                pourcentage_arrets = row.get('pourcentage_arrets', 0)
                gk_clean_sheets = row.get('gk_clean_sheets', 0)
                saves = row.get('saves', 0)
                
                if pourcentage_arrets > 70:
                    bonus += (pourcentage_arrets - 70) * 500_000
                if gk_clean_sheets > 10:
                    bonus += gk_clean_sheets * 300_000
                if saves > 50:
                    bonus += (saves - 50) * 100_000
                    
            elif position in ['DF', 'MFDF']:
                tacles_reussis = row.get('tacles_reussis', 0)
                interceptions = row.get('interceptions', 0)
                clean_sheets = row.get('clean_sheets', 0)
                duels_aeriens = row.get('duels_aeriens', 0)
                
                if tacles_reussis > 30:
                    bonus += (tacles_reussis - 30) * 200_000
                if interceptions > 20:
                    bonus += (interceptions - 20) * 150_000
                if clean_sheets > 10:
                    bonus += clean_sheets * 400_000
                if duels_aeriens > 30:
                    bonus += (duels_aeriens - 30) * 100_000
                    
            elif position == 'MF':
                passes_reussies = row.get('passes_reussies', 0)
                passes_cle = row.get('passes_cle', 0)
                passes_decisives = row.get('passes_decisives', 0)
                mid_buts = row.get('mid_buts', 0)
                
                if passes_reussies > 1000:
                    bonus += (passes_reussies - 1000) * 10_000
                if passes_cle > 5:
                    bonus += passes_cle * 1_000_000
                if passes_decisives > 3:
                    bonus += passes_decisives * 2_000_000
                if mid_buts > 5:
                    bonus += mid_buts * 1_500_000
                    
            elif position == 'FW':
                att_buts = row.get('att_buts', 0)
                taux_conversion = row.get('taux_conversion', 0)
                att_passes = row.get('att_passes', 0)
                shots_total = row.get('shots_total', 0)
                
                if att_buts > 15:
                    bonus += (att_buts - 15) * 2_000_000
                elif att_buts > 10:
                    bonus += (att_buts - 10) * 1_500_000
                if taux_conversion > 0.15:
                    bonus += (taux_conversion - 0.15) * 50_000_000
                if att_passes > 5:
                    bonus += att_passes * 1_000_000
                if shots_total > 80:
                    bonus += (shots_total - 80) * 200_000
            
            return max(bonus, 0)
            
        except Exception as e:
            logger.error(f"Erreur calcul bonus performance: {e}")
            return 0

    def calculate_penalty_malus(self, row):
        """Calcule les malus (cartons, peu de temps de jeu, etc.)"""
        try:
            malus = 0
            
            cartons_jaunes = row.get('cartons_jaunes', 0)
            cartons_rouge = row.get('cartons_rouge', 0)
            
            if cartons_jaunes > 5:
                malus += (cartons_jaunes - 5) * 500_000
            if cartons_rouge > 0:
                malus += cartons_rouge * 3_000_000
                
            minutes = max(
                row.get('def_minutes', 0),
                row.get('mid_minutes', 0),
                row.get('att_minutes', 0),
                row.get('gk_matchs', 0) * 90
            )
            
            if minutes < 1000:
                malus += (1000 - minutes) * 10_000
            
            return max(malus, 0)
            
        except Exception as e:
            logger.error(f"Erreur calcul malus: {e}")
            return 0

    def calculate_market_value(self, row):
        """Calcule la valeur marchande totale d'un joueur"""
        try:
            base_value = self.calculate_base_value_by_position(row)
            performance_bonus = self.calculate_performance_bonus(row)
            penalty_malus = self.calculate_penalty_malus(row)
            total_value = max(base_value + performance_bonus - penalty_malus, 1_000_000)
            return int(total_value)
        except Exception as e:
            logger.error(f"Erreur calcul valeur pour joueur {row.get('name', 'Unknown')}: {e}")
            return 10_000_000

    def prepare_data(self, df):
        """Prépare les données pour l'entraînement du modèle"""
        try:
            if df is None or df.empty:
                logger.error("Données non disponibles ou vides")
                return None, None

            # Créer une copie pour éviter les modifications sur l'original
            df_work = df.copy()

            # Calculer la valeur marchande réelle pour chaque joueur
            df_work['market_value'] = df_work.apply(self.calculate_market_value, axis=1)

            # Sélectionner les features numériques
            numeric_features = [
                'age', 'tacles_reussis', 'interceptions', 'duels_aeriens', 'duels_gagnes',
                'clean_sheets', 'cartons_jaunes', 'cartons_rouge', 'def_minutes',
                'pourcentage_arrets', 'gk_clean_sheets', 'sorties', 'penalties_arretes',
                'gk_matchs', 'saves', 'passes_reussies', 'recuperations', 'distance_parcourue',
                'passes_cle', 'passes_decisives', 'mid_buts', 'mid_minutes', 'att_buts',
                'taux_conversion', 'att_passes', 'att_minutes', 'shots_total'
            ]

            # Vérifier que toutes les colonnes existent
            existing_features = [col for col in numeric_features if col in df_work.columns]
            if len(existing_features) != len(numeric_features):
                missing = set(numeric_features) - set(existing_features)
                logger.warning(f"Colonnes manquantes: {missing}")

            # Encoder les positions et leagues (one-hot encoding)
            df_encoded = pd.get_dummies(df_work, columns=['position', 'league'], dummy_na=False)

            # Préparer les colonnes de features
            categorical_features = [col for col in df_encoded.columns 
                                  if col.startswith('position_') or col.startswith('league_')]
            
            all_features = existing_features + categorical_features
            self.feature_columns = all_features

            # Préparer X (features) et y (target)
            X = df_encoded[all_features]
            y = df_encoded['market_value']

            # Vérifier qu'il n'y a pas de valeurs infinies ou NaN
            X = X.replace([np.inf, -np.inf], 0).fillna(0)
            y = y.replace([np.inf, -np.inf], 0).fillna(0)

            # Normaliser les features
            X_scaled = self.scaler.fit_transform(X)

            logger.info(f"Données préparées: {X_scaled.shape[0]} échantillons, {X_scaled.shape[1]} features")
            return X_scaled, y

        except Exception as e:
            logger.error(f"Erreur lors de la préparation des données: {e}")
            return None, None

    def build_model(self, input_dim):
        """Construit le modèle de deep learning"""
        try:
            model = Sequential([
                Dense(256, activation='relu', input_shape=(input_dim,)),
                Dropout(0.4),
                Dense(128, activation='relu'),
                Dropout(0.3),
                Dense(64, activation='relu'),
                Dropout(0.2),
                Dense(32, activation='relu'),
                Dense(1, activation='linear')  # Sortie linéaire pour la valeur marchande
            ])

            model.compile(
                optimizer=Adam(learning_rate=0.0005), 
                loss='mse', 
                metrics=['mae']
            )
            
            logger.info(f"Modèle créé avec {input_dim} features d'entrée")
            return model
            
        except Exception as e:
            logger.error(f"Erreur lors de la construction du modèle: {e}")
            return None

    def train_and_evaluate(self):
        """Entraîne le modèle avec validation croisée et évalue les performances"""
        try:
            # Récupérer les données
            df = self.get_all_players_data()
            if df is None or df.empty:
                logger.error("Impossible de récupérer les données")
                return None

            # Préparer les données
            X, y = self.prepare_data(df)
            if X is None or y is None:
                logger.error("Impossible de préparer les données")
                return None

            if len(X) < 10:
                logger.error("Pas assez de données pour l'entraînement")
                return None

            # Validation croisée
            kfold = KFold(n_splits=5, shuffle=True, random_state=42)
            mae_scores = []
            rmse_scores = []
            r2_scores = []

            for fold, (train_idx, val_idx) in enumerate(kfold.split(X), 1):
                X_train, X_val = X[train_idx], X[val_idx]
                y_train, y_val = y.iloc[train_idx], y.iloc[val_idx]

                # Construire le modèle
                model = self.build_model(X_train.shape[1])
                if model is None:
                    continue

                logger.info(f"Début de l'entraînement pour le fold {fold} avec {len(X_train)} échantillons")

                # Entraîner le modèle
                history = model.fit(
                    X_train, y_train,
                    validation_data=(X_val, y_val),
                    epochs=100,
                    batch_size=min(32, len(X_train) // 4),
                    verbose=0,
                    shuffle=True
                )

                # Évaluer le modèle
                y_pred = model.predict(X_val, verbose=0)
                mae = np.mean(np.abs(y_val - y_pred.flatten()))
                rmse = math.sqrt(mean_squared_error(y_val, y_pred.flatten()))
                r2 = r2_score(y_val, y_pred.flatten())

                mae_scores.append(mae)
                rmse_scores.append(rmse)
                r2_scores.append(r2)

                logger.info(f"Fold {fold}: MAE = {mae:,.2f}, RMSE = {rmse:,.2f}, R² = {r2:.4f}")

            # Moyennes des métriques
            self.mean_mae = np.mean(mae_scores)  # Store as class variable
            mean_rmse = np.mean(rmse_scores)
            mean_r2 = np.mean(r2_scores)
            std_mae = np.std(mae_scores)

            logger.info(f"Performance moyenne (5-fold CV): MAE = {self.mean_mae:,.2f} ± {std_mae:,.2f}, "
                        f"RMSE = {mean_rmse:,.2f}, R² = {mean_r2:.4f}")

            self.model = model  # Sauvegarder le dernier modèle entraîné
            return model

        except Exception as e:
            logger.error(f"Erreur lors de l'entraînement et évaluation: {e}")
            return None

    def predict_value(self, model, player_data):
        """Prédit la valeur marchande pour un joueur donné avec un intervalle de confiance"""
        try:
            if model is None:
                logger.error("Modèle non fourni")
                return 10_000_000, 0

            if not isinstance(player_data, dict):
                logger.error("Les données du joueur doivent être un dictionnaire")
                return 10_000_000, 0

            # Créer un DataFrame à partir des données du joueur
            player_df = pd.DataFrame([player_data])
            
            # Encoder les variables catégorielles
            player_df = pd.get_dummies(player_df, columns=['position', 'league'], dummy_na=False)
            
            # S'assurer que toutes les colonnes de features sont présentes
            for col in self.feature_columns:
                if col not in player_df.columns:
                    player_df[col] = 0

            # Réorganiser les colonnes dans le bon ordre
            X_player = player_df[self.feature_columns]
            
            # Remplacer les valeurs infinies et NaN
            X_player = X_player.replace([np.inf, -np.inf], 0).fillna(0)
            
            # Normaliser avec le scaler entraîné
            X_player_scaled = self.scaler.transform(X_player)

            # Prédire la valeur et estimer la variance
            predicted_value = model.predict(X_player_scaled, verbose=0)[0][0]
            confidence_interval = 2 * self.mean_mae if self.mean_mae > 0 else 0  # Use class variable

            if predicted_value == 10_000_000:
                logger.warning("Predicted value hit minimum threshold, possible underfitting or data mismatch")

            logger.info(f"Prédiction: {int(predicted_value):,} €, Intervalle de confiance: ±{int(confidence_interval):,} €")
            
            final_value = max(int(predicted_value), 1_000_000)
            return final_value, confidence_interval

        except Exception as e:
            logger.error(f"Erreur lors de la prédiction: {e}")
            return 10_000_000, 0

    def __del__(self):
        """Ferme la connexion à la destruction de l'instance"""
        try:
            self._close_db_connection()
        except:
            pass

# Exemple d'utilisation
if __name__ == "__main__":
    try:
        # Instancier et entraîner le modèle
        predictor = PlayerValuePredictor()
        model = predictor.train_and_evaluate()

        if model is not None:
            # Récupérer les données réelles
            df = predictor.get_all_players_data()
            if df is not None and not df.empty:
                # Sélectionner les 10 premiers joueurs
                top_10_players = df.iloc[:10].to_dict(orient='records')
                
                for real_player in top_10_players:
                    # Préparer les données du joueur pour la prédiction
                    player_data = {
                        key: real_player.get(key, 0) for key in [
                            'age', 'position', 'league', 'tacles_reussis', 'interceptions', 
                            'def_minutes', 'pourcentage_arrets', 'gk_clean_sheets', 'saves', 
                            'passes_reussies', 'passes_cle', 'passes_decisives', 'mid_buts', 
                            'mid_minutes', 'att_buts', 'taux_conversion', 'att_passes', 
                            'att_minutes', 'shots_total', 'cartons_jaunes', 'cartons_rouge',
                            'duels_aeriens', 'duels_gagnes', 'clean_sheets', 'sorties',
                            'penalties_arretes', 'gk_matchs', 'recuperations', 'distance_parcourue'
                        ]
                    }
                    
                    # Calculer la valeur avec la formule heuristique
                    heuristic_value = predictor.calculate_market_value(real_player)
                    
                    # Prédire avec le modèle ML et obtenir l'intervalle de confiance
                    predicted_value, confidence_interval = predictor.predict_value(model, player_data)
                    
                    # Ajouter un timestamp pour traçabilité
                    from datetime import datetime
                    current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    logger.info(f"Prediction results for {real_player.get('name', 'Joueur inconnu')} at {current_time} CET")
                    
                    print(f"\n=== Résultats pour {real_player.get('name', 'Joueur inconnu')} ===")
                    print(f"Date et heure: {current_time} CET")
                    print(f"Position: {real_player.get('position', 'N/A')}")
                    print(f"Âge: {real_player.get('age', 'N/A')} ans")
                    print(f"Ligue: {real_player.get('league', 'N/A')}")
                    print(f"Valeur heuristique: {heuristic_value:,} €")
                    print(f"Valeur prédite (ML): {predicted_value:,} €")
                    print(f"Intervalle de confiance: ±{int(confidence_interval):,} €")
                    print(f"Différence avec heuristique: {abs(heuristic_value - predicted_value):,} €")
                    
            else:
                print("Aucun joueur trouvé dans la base de données")
        else:
            print("Échec de l'entraînement du modèle")
            
    except Exception as e:
        logger.error(f"Erreur dans l'exécution principale: {e}")
        print(f"Erreur: {e}")