import pandas as pd
import numpy as np
from datetime import datetime
from decimal import Decimal
import logging
import psycopg2

# Configuration des logs
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class FootballMarketValueCalculator:
    def __init__(self, host, database, username, password, port=5432):
        """Initialise la connexion à la base de données"""
        self.connection_params = {
            'host': host,
            'database': database,
            'user': username,
            'password': password,
            'port': port
        }
        self.conn = None
        self.cursor = None
    
    def connect_to_database(self):
        """Établit la connexion à PostgreSQL"""
        try:
            # Utiliser UTF-8 par défaut (plus compatible)
            self.conn = psycopg2.connect(**self.connection_params)
            self.cursor = self.conn.cursor()
            
            # S'assurer que la connexion utilise UTF-8
            self.cursor.execute("SET CLIENT_ENCODING TO 'UTF8';")
            self.conn.commit()
            
            logger.info("Connexion à la base de données réussie avec encodage UTF-8")
            return True
        except psycopg2.Error as e:
            logger.error(f"Erreur de connexion: {e}")
            # Essayer avec d'autres paramètres d'encodage
            try:
                connection_params_alt = {
                    **self.connection_params,
                    'options': '-c client_encoding=utf8'
                }
                self.conn = psycopg2.connect(**connection_params_alt)
                self.cursor = self.conn.cursor()
                logger.info("Connexion réussie avec paramètres UTF-8 alternatifs")
                return True
            except psycopg2.Error as e2:
                logger.error(f"Erreur de connexion alternative: {e2}")
                return False
    
    def disconnect(self):
        """Ferme la connexion à la base de données"""
        if self.cursor:
            self.cursor.close()
        if self.conn:
            self.conn.close()
        logger.info("Connexion fermée")
    
    def get_all_players_data(self):
        """Récupère toutes les données des joueurs avec leurs statistiques"""
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
            d.tacles_reussis,
            d.interceptions,
            d.duels_aeriens,
            d.duels_gagnes,
            d.clean_sheets,
            d.cartons_jaunes,
            d.cartons_rouge,
            d.minutes_jouees as def_minutes,
            -- Statistiques gardiens
            g.pourcentage_arrets,
            g.clean_sheets as gk_clean_sheets,
            g.sorties,
            g.penalties_arretes,
            g.matchs_joues as gk_matchs,
            g.saves,
            -- Statistiques milieux
            m.passes_reussies,
            m.recuperations,
            m.distance_parcourue,
            m.passes_cle,
            m.passes_decisives,
            m.buts_marques as mid_buts,
            m.minutes_jouees as mid_minutes,
            -- Statistiques attaquants
            a.buts_marques as att_buts,
            a.taux_conversion,
            a.passes_decisives as att_passes,
            a.minutes_jouees as att_minutes,
            a.shots_total
        FROM public.players p
        LEFT JOIN public.clubs c ON p.club_id = c.id
        LEFT JOIN public.defenseurs d ON p.id = d.joueur_id
        LEFT JOIN public.gardiens g ON p.id = g.joueur_id
        LEFT JOIN public.milieux m ON p.id = m.joueur_id
        LEFT JOIN public.attaquants a ON p.id = a.joueur_id
        ORDER BY p.id ASC;
        """
        
        try:
            self.cursor.execute(query)
            
            columns = [desc[0] for desc in self.cursor.description]
            data = self.cursor.fetchall()
            
            df = pd.DataFrame(data, columns=columns)
            logger.info(f"Récupération de {len(df)} joueurs avec leurs statistiques")
            return df
        except Exception as e:
            logger.error(f"Erreur lors de la récupération des données: {e}")
            return None
    
    def calculate_base_value_by_position(self, row):
        """Calcule la valeur de base selon la position avec un bonus pour les minutes jouées"""
        position = row['position']
        age = row['age']
        league = row['league']
        
        # Coefficients de base par position
        base_values = {
            'GK': 15_000_000,  # 15M euros
            'DF': 25_000_000,  # 25M euros
            'MF': 30_000_000,  # 30M euros
            'FW': 40_000_000,  # 40M euros
            'MFDF': 28_000_000  # 28M euros pour les milieux defensifs
        }
        
        # Coefficient de ligue
        league_multipliers = {
            'Premier League': 1.5,
            'La Liga': 1.4,
            'Serie A': 1.3,
            'Bundesliga': 1.3,
            'Ligue 1': 1.2,
            'Eredivisie': 0.8,
            'Primeira Liga': 0.7
        }
        
        # Coefficient d'âge
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
        
        # Récupérer les minutes jouées (priorité selon la position)
        minutes = row.get('def_minutes') or row.get('mid_minutes') or row.get('att_minutes') or row.get('gk_matchs') * 90
        if pd.notna(minutes):
            # Bonus logarithmique pour les minutes jouées (plus d'impact pour les hautes valeurs)
            minutes_bonus = 10_000_000 * np.log1p(minutes / 1000)  # Logarithme pour un effet progressif
            minutes_bonus = min(minutes_bonus, 20_000_000)  # Limite à 20M€ pour éviter des valeurs excessives
        else:
            minutes_bonus = 0
        
        base_value = base_values.get(position, 20_000_000)
        league_mult = league_multipliers.get(league, 1.0)
        
        return (base_value + minutes_bonus) * league_mult * age_multiplier
    
    def calculate_performance_bonus(self, row):
        """Calcule le bonus de performance selon les statistiques"""
        position = row['position']
        bonus = 0
        
        if position == 'GK':
            if pd.notna(row['pourcentage_arrets']) and row['pourcentage_arrets'] > 70:
                bonus += (row['pourcentage_arrets'] - 70) * 500_000
            if pd.notna(row['gk_clean_sheets']) and row['gk_clean_sheets'] > 10:
                bonus += row['gk_clean_sheets'] * 300_000
            if pd.notna(row['saves']) and row['saves'] > 50:
                bonus += (row['saves'] - 50) * 100_000
                
        elif position in ['DF', 'MFDF']:
            if pd.notna(row['tacles_reussis']) and row['tacles_reussis'] > 30:
                bonus += (row['tacles_reussis'] - 30) * 200_000
            if pd.notna(row['interceptions']) and row['interceptions'] > 20:
                bonus += (row['interceptions'] - 20) * 150_000
            if pd.notna(row['clean_sheets']) and row['clean_sheets'] > 10:
                bonus += row['clean_sheets'] * 400_000
            if pd.notna(row['duels_aeriens']) and row['duels_aeriens'] > 30:
                bonus += (row['duels_aeriens'] - 30) * 100_000
                
        elif position == 'MF':
            if pd.notna(row['passes_reussies']) and row['passes_reussies'] > 1000:
                bonus += (row['passes_reussies'] - 1000) * 10_000
            if pd.notna(row['passes_cle']) and row['passes_cle'] > 5:
                bonus += row['passes_cle'] * 1_000_000
            if pd.notna(row['passes_decisives']) and row['passes_decisives'] > 3:
                bonus += row['passes_decisives'] * 2_000_000
            if pd.notna(row['mid_buts']) and row['mid_buts'] > 5:
                bonus += row['mid_buts'] * 1_500_000
                
        elif position == 'FW':
            if pd.notna(row['att_buts']) and row['att_buts'] > 15:
                bonus += (row['att_buts'] - 15) * 2_000_000
            elif pd.notna(row['att_buts']) and row['att_buts'] > 10:
                bonus += (row['att_buts'] - 10) * 1_500_000
            if pd.notna(row['taux_conversion']) and row['taux_conversion'] > 0.15:
                bonus += (row['taux_conversion'] - 0.15) * 50_000_000
            if pd.notna(row['att_passes']) and row['att_passes'] > 5:
                bonus += row['att_passes'] * 1_000_000
            if pd.notna(row['shots_total']) and row['shots_total'] > 80:
                bonus += (row['shots_total'] - 80) * 200_000
        
        return bonus
    
    def calculate_penalty_malus(self, row):
        """Calcule les malus (cartons, peu de temps de jeu, etc.)"""
        malus = 0
        
        # Malus cartons
        if pd.notna(row['cartons_jaunes']) and row['cartons_jaunes'] > 5:
            malus += (row['cartons_jaunes'] - 5) * 500_000
        if pd.notna(row['cartons_rouge']) and row['cartons_rouge'] > 0:
            malus += row['cartons_rouge'] * 3_000_000
            
        # Malus temps de jeu insuffisant (plus progressif)
        minutes = row.get('def_minutes') or row.get('mid_minutes') or row.get('att_minutes') or row.get('gk_matchs') * 90
        if pd.notna(minutes) and minutes < 1000:
            malus += (1000 - minutes) * 10_000  # Augmentation du malus pour accentuer l'impact
        
        return malus
    
    def calculate_market_value(self, row):
        """Calcule la valeur marchande totale d'un joueur"""
        try:
            # Valeur de base (inclut maintenant les minutes jouées)
            base_value = self.calculate_base_value_by_position(row)
            
            # Bonus de performance
            performance_bonus = self.calculate_performance_bonus(row)
            
            # Malus
            penalty_malus = self.calculate_penalty_malus(row)
            
            # Valeur finale
            total_value = max(base_value + performance_bonus - penalty_malus, 1_000_000)  # Minimum 1M€
            
            return int(total_value)
            
        except Exception as e:
            logger.error(f"Erreur calcul valeur pour joueur {row.get('name', 'Unknown')}: {e}")
            return 10_000_000  # Valeur par défaut
    
    def format_market_value(self, value):
        """Formate la valeur marchande en string"""
        if value >= 1_000_000:
            formatted_value = f"€{value/1_000_000:.2f}m"
        else:
            formatted_value = f"€{value/1_000:.0f}k"
        
        current_date = datetime.now().strftime("%B %d, %Y")
        return f"{formatted_value} Last update: {current_date}"
    
    def calculate_all_market_values(self, df):
        """Calcule les valeurs marchandes pour tous les joueurs"""
        logger.info("Début du calcul des valeurs marchandes...")
        
        # Calcul des valeurs
        df['calculated_value'] = df.apply(self.calculate_market_value, axis=1)  # Valeur numérique (e.g., 99500000)
        df['formatted_market_value'] = df['calculated_value'].apply(self.format_market_value)  # Texte (e.g., "€99.50m Last update: July 25, 2025")
        
        logger.info(f"Calcul terminé pour {len(df)} joueurs")
        return df
    def update_market_values_in_database(self, df):
        """Met à jour les valeurs marchandes dans la base de données"""
        logger.info("Début de la mise à jour des valeurs dans la base...")
        
        # Vérifier si les colonnes existent
        check_column_query = """
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'players' AND column_name IN ('market_value', 'market_value_numeric')
        """
        
        self.cursor.execute(check_column_query)
        existing_columns = {row[0] for row in self.cursor.fetchall()}
        
        if 'market_value' not in existing_columns:
            alter_query = "ALTER TABLE public.players ADD COLUMN market_value TEXT"
            self.cursor.execute(alter_query)
            logger.info("Colonne market_value ajoutée")
        
        if 'market_value_numeric' not in existing_columns:
            alter_query = "ALTER TABLE public.players ADD COLUMN market_value_numeric INTEGER"
            self.cursor.execute(alter_query)
            logger.info("Colonne market_value_numeric ajoutée")
        
        # Préparer la requête de mise à jour pour les deux colonnes
        update_query = """
        UPDATE public.players 
        SET market_value = %s, market_value_numeric = %s 
        WHERE id = %s
        """
        
        # Effectuer les mises à jour par batch
        updates_data = [
            (row['formatted_market_value'], row['calculated_value'], row['id']) 
            for _, row in df.iterrows()
        ]
        
        try:
            self.cursor.executemany(update_query, updates_data)
            self.conn.commit()
            logger.info(f"Mise à jour réussie pour {len(updates_data)} joueurs")
            return True
        except Exception as e:
            self.conn.rollback()
            logger.error(f"Erreur lors de la mise à jour: {e}")
            return False
    
    def generate_summary_report(self, df):
        """Génère un rapport de synthèse"""
        logger.info("\n=== RAPPORT DE SYNTHÈSE ===")
        logger.info(f"Nombre total de joueurs traités: {len(df)}")
        
        # Statistiques par position
        position_stats = df.groupby('position').agg({
            'calculated_value': ['count', 'mean', 'min', 'max']
        }).round(2)
        
        logger.info("\nStatistiques par position:")
        for position in df['position'].unique():
            if pd.notna(position):
                pos_data = df[df['position'] == position]
                avg_value = pos_data['calculated_value'].mean()
                count = len(pos_data)
                logger.info(f"{position}: {count} joueurs, valeur moyenne: €{avg_value/1_000_000:.2f}M")
        
        # Top 10 des joueurs les plus chers
        top_players = df.nlargest(10, 'calculated_value')[['name', 'position', 'club_name', 'formatted_market_value']]
        logger.info(f"\nTop 10 des joueurs les plus chers:")
        for _, player in top_players.iterrows():
            logger.info(f"{player['name']} ({player['position']}) - {player['club_name']}: {player['formatted_market_value']}")
        
        return df
    
    def run_complete_process(self):
        """Exécute le processus complet"""
        if not self.connect_to_database():
            return False
        
        try:
            # 1. Récupérer les données
            df = self.get_all_players_data()
            if df is None or df.empty:
                logger.error("Aucune donnée récupérée")
                return False
            
            # 2. Calculer les valeurs marchandes
            df = self.calculate_all_market_values(df)
            
            # 3. Mettre à jour la base de données
            if self.update_market_values_in_database(df):
                # 4. Générer le rapport
                self.generate_summary_report(df)
                logger.info("Processus terminé avec succès!")
                return True
            else:
                return False
                
        except Exception as e:
            logger.error(f"Erreur dans le processus complet: {e}")
            return False
        finally:
            self.disconnect()

# UTILISATION DU CODE
def main():
    """Fonction principale"""

    DB_CONFIG = {
        'host': 'localhost',  
        'database': 'football_db',  
        'username': 'postgres', 
        'password': 'adminadmin',  
        'port': 5432
    }
    
    # Créer une instance du calculateur
    calculator = FootballMarketValueCalculator(**DB_CONFIG)
    
    # Exécuter le processus complet
    success = calculator.run_complete_process()
    
    if success:
        print("✅ Calcul et mise à jour des valeurs marchandes terminés avec succès!")
    else:
        print("❌ Erreur lors du processus de calcul des valeurs marchandes")

if __name__ == "__main__":
    main()