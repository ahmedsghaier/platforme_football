import os
import re
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, WebDriverException
from bs4 import BeautifulSoup
import psycopg2
from psycopg2 import pool
from psycopg2.extras import execute_batch
from selenium_stealth import stealth
from webdriver_manager.chrome import ChromeDriverManager
import logging
import time
import random

# Suppress TensorFlow Lite and other verbose logging
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
os.environ['TF_DISABLE_MLIR'] = '1'

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class FBrefScraperSelenium:
    def __init__(self, db_config):
        self.db_config = db_config
        self.driver = None
        self.wait = None
        self.db_pool = None
        self.setup_driver()
        self.setup_db_pool()

    def setup_driver(self):
        try:
            chrome_options = Options()
            chrome_options.add_argument("--headless")
            chrome_options.add_argument("--disable-gpu")
            chrome_options.add_argument("--no-sandbox")
            chrome_options.add_argument("--disable-dev-shm-usage")
            chrome_options.add_argument("--window-size=1920,1080")
            chrome_options.add_argument(
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            chrome_options.add_argument("--disable-blink-features=AutomationControlled")
            chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
            chrome_options.add_experimental_option('useAutomationExtension', False)
            chrome_options.add_argument("--disable-notifications")
            chrome_options.add_argument(f"--user-data-dir={os.path.join(os.getcwd(), 'chrome_profile')}")

            service = Service(ChromeDriverManager().install(), log_path=os.devnull)
            self.driver = webdriver.Chrome(service=service, options=chrome_options)

            stealth(
                self.driver,
                languages=["en-US", "en"],
                vendor="Google Inc.",
                platform="Win32",
                webgl_vendor="Intel Inc.",
                renderer="Intel Iris OpenGL Engine",
                fix_hairline=True
            )
            self.driver.execute_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
            self.wait = WebDriverWait(self.driver, 30)
            logger.info("Chrome driver initialized successfully")
        except WebDriverException as e:
            logger.error(f"Failed to initialize Chrome driver: {e}")
            raise

    def setup_db_pool(self):
        try:
            self.db_pool = psycopg2.pool.SimpleConnectionPool(1, 10, **self.db_config)
            logger.info("Database connection pool initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize database connection pool: {e}")
            raise

    def get_db_connection(self):
        try:
            return self.db_pool.getconn()
        except Exception as e:
            logger.error(f"Error getting database connection: {e}")
            raise

    def release_db_connection(self, conn):
        self.db_pool.putconn(conn)

    def create_tables(self):
        conn = self.get_db_connection()
        try:
            with conn.cursor() as cur:
                # Table des clubs
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS clubs (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        league VARCHAR(100),
                        level VARCHAR(50),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """)
                
                # Table des joueurs
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        age INTEGER CHECK (age >= 0),
                        position VARCHAR(50),
                        nationality VARCHAR(50),
                        club_id INTEGER REFERENCES clubs(id) ON DELETE CASCADE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT players_name_club_unique UNIQUE (name, club_id)
                    );
                """)
                cur.execute("""
                ALTER TABLE players ADD COLUMN IF NOT EXISTS image VARCHAR(255);
            """)
                
                # Table des gardiens
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS gardiens (
                        id SERIAL PRIMARY KEY,
                        joueur_id INTEGER REFERENCES players(id) ON DELETE CASCADE UNIQUE,
                        pourcentage_arrets FLOAT CHECK (pourcentage_arrets BETWEEN 0 AND 100),
                        clean_sheets INTEGER CHECK (clean_sheets >= 0),
                        sorties INTEGER CHECK (sorties >= 0),
                        penalties_arretes INTEGER CHECK (penalties_arretes >= 0),
                        matchs_joues INTEGER CHECK (matchs_joues >= 0),
                        saves INTEGER CHECK (saves >= 0),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """)
                
                # Table des défenseurs
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS defenseurs (
                        id SERIAL PRIMARY KEY,
                        joueur_id INTEGER REFERENCES players(id) ON DELETE CASCADE UNIQUE,
                        tacles_reussis INTEGER CHECK (tacles_reussis >= 0),
                        interceptions INTEGER CHECK (interceptions >= 0),
                        duels_aeriens INTEGER CHECK (duels_aeriens >= 0),
                        duels_gagnes INTEGER CHECK (duels_gagnes >= 0),
                        clean_sheets INTEGER CHECK (clean_sheets >= 0),
                        cartons_jaunes INTEGER CHECK (cartons_jaunes >= 0),
                        cartons_rouge INTEGER CHECK (cartons_rouge >= 0),
                        minutes_jouees INTEGER CHECK (minutes_jouees >= 0),
                        tackles INTEGER CHECK (tackles >= 0),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """)
                
                # Table des milieux
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS milieux (
                        id SERIAL PRIMARY KEY,
                        joueur_id INTEGER REFERENCES players(id) ON DELETE CASCADE UNIQUE,
                        passes_reussies INTEGER CHECK (passes_reussies >= 0),
                        recuperations INTEGER CHECK (recuperations >= 0),
                        distance_parcourue FLOAT CHECK (distance_parcourue >= 0),
                        passes_cle FLOAT CHECK (passes_cle >= 0),
                        passes_decisives INTEGER CHECK (passes_decisives >= 0),
                        buts_marques INTEGER CHECK (buts_marques >= 0),
                        minutes_jouees INTEGER CHECK (minutes_jouees >= 0),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """)
                
                # Table des attaquants
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS attaquants (
                        id SERIAL PRIMARY KEY,
                        joueur_id INTEGER REFERENCES players(id) ON DELETE CASCADE UNIQUE,
                        buts_marques INTEGER CHECK (buts_marques >= 0),
                        taux_conversion FLOAT CHECK (taux_conversion >= 0),
                        passes_decisives INTEGER CHECK (passes_decisives >= 0),
                        minutes_jouees INTEGER CHECK (minutes_jouees >= 0),
                        shots_total INTEGER CHECK (shots_total >= 0),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """)
                
                # Create indexes for performance
                cur.execute("""
                    CREATE INDEX IF NOT EXISTS idx_players_club_id_name ON players(club_id, name);
                    CREATE INDEX IF NOT EXISTS idx_players_club_id ON players(club_id);
                    CREATE INDEX IF NOT EXISTS idx_players_name ON players(name);
                    CREATE INDEX IF NOT EXISTS idx_clubs_name ON clubs(name);
                    CREATE INDEX IF NOT EXISTS idx_gardiens_joueur_id ON gardiens(joueur_id);
                    CREATE INDEX IF NOT EXISTS idx_defenseurs_joueur_id ON defenseurs(joueur_id);
                    CREATE INDEX IF NOT EXISTS idx_milieux_joueur_id ON milieux(joueur_id);
                    CREATE INDEX IF NOT EXISTS idx_attaquants_joueur_id ON attaquants(joueur_id);
                """)
                
                conn.commit()
                logger.info("Database tables created successfully")
        except Exception as e:
            logger.error(f"Error creating tables: {e}")
            conn.rollback()
        finally:
            self.release_db_connection(conn)

    def get_teams(self, league_url):
        try:
            self.driver.get(league_url)
            self.wait.until(EC.presence_of_element_located((By.TAG_NAME, "table")))
            soup = BeautifulSoup(self.driver.page_source, 'html.parser')
            table = soup.find('table', class_='stats_table')
            teams = []
            if table:
                for row in table.tbody.find_all('tr'):
                    link = row.find('a')
                    if link and link.get('href'):
                        teams.append({
                            'name': link.text.strip(),
                            'url': 'https://fbref.com' + link['href']
                        })
                logger.info(f"Found {len(teams)} teams in league")
            else:
                logger.warning(f"No stats_table found at {league_url}")
            return teams
        except TimeoutException:
            logger.error(f"Timeout loading league page: {league_url}")
            return []
        except Exception as e:
            logger.error(f"Error scraping teams from {league_url}: {e}")
            return []

    def find_column_indices(self, headers):
        """Détection améliorée des colonnes avec data-stat"""
        indices = {
            'name': None,
            'nationality': None,
            'position': None,
            'age': None,
            'minutes': None,
            'goals': None,
            'assists': None,
            'tackles': None,
            'interceptions': None,
            'passes': None,
            'passes_completed': None,
            'cards_yellow': None,
            'cards_red': None
        }
        
        for i, header in enumerate(headers):
            if not header:
                continue
                
            header_lower = header.lower().strip()
            
            # Détection basée sur data-stat (plus fiable pour FBref)
            if header_lower == 'player':
                indices['name'] = i
            elif header_lower in ['nation', 'nationality']:
                indices['nationality'] = i
            elif header_lower in ['pos', 'position']:
                indices['position'] = i
            elif header_lower == 'age':
                indices['age'] = i
            elif header_lower in ['min', 'minutes']:
                indices['minutes'] = i
            elif header_lower in ['gls', 'goals']:
                indices['goals'] = i
            elif header_lower in ['ast', 'assists']:
                indices['assists'] = i
            elif header_lower in ['cs', 'clean_sheets']:
                indices['clean_sheets'] = i
            elif header_lower in ['saves', 'sv']:
                indices['saves'] = i
            elif header_lower in ['tkl', 'tackles']:
                indices['tackles'] = i
            elif header_lower in ['int', 'interceptions']:
                indices['interceptions'] = i
            elif header_lower in ['cmp', 'passes', 'pass_completed']:
                indices['passes'] = i
            elif header_lower in ['pct', 'pass_pct', 'pass_completion']:
                indices['pass_completion'] = i
            elif header_lower in ['crdy', 'yellow_cards']:
                indices['cards_yellow'] = i
            elif header_lower in ['crdr', 'red_cards']:
                indices['cards_red'] = i
        
        # Détection de fallback basée sur le texte
        if indices['name'] is None:
            for i, header in enumerate(headers):
                if not header:
                    continue
                header_lower = header.lower().strip()
                if 'player' in header_lower or header_lower == 'name':
                    indices['name'] = i
                    break
        
        # Si toujours pas trouvé, utiliser la première colonne
        if indices['name'] is None:
            indices['name'] = 0
            
        return indices

    def clean_nationality(self, nationality_text):
        """Nettoie et formate la nationalité pour ne garder que les lettres majuscules"""
        if not nationality_text:
            return 'Unknown'
        
        # Supprimer les espaces en début et fin
        nationality_text = nationality_text.strip()
        
        # Extraire uniquement les lettres majuscules
        uppercase_letters = ''.join([char for char in nationality_text if char.isupper()])
        
        # Si on a des lettres majuscules, les utiliser
        if uppercase_letters and len(uppercase_letters) >= 2:
            return uppercase_letters
        
        # Sinon, essayer de convertir le texte en majuscules si c'est un code pays valide
        if nationality_text.isalpha() and len(nationality_text) >= 2:
            return nationality_text.upper()
        
        # Par défaut, retourner Unknown
        return 'Unknown'

    def safe_int(self, value):
        """Convertit une valeur en entier de manière sécurisée"""
        if not value or value == '':
            return None
        try:
            # Nettoyer la valeur (supprimer les virgules, espaces, etc.)
            clean_value = str(value).replace(',', '').strip()
            if clean_value == '' or clean_value == '-':
                return None
            return int(float(clean_value))
        except (ValueError, TypeError):
            return None

    def safe_float(self, value):
        """Convertit une valeur en float de manière sécurisée"""
        if not value or value == '':
            return None
        try:
            clean_value = str(value).replace(',', '').strip()
            if clean_value == '' or clean_value == '-':
                return None
            return float(clean_value)
        except (ValueError, TypeError):
            return None
    
    def extract_player_data(self, row, column_indices, existing_player_data=None):
        """Extraction ou fusion des données joueur depuis une ligne HTML avec les bonnes colonnes"""
        cells = row.find_all(['td', 'th'])
        if len(cells) == 0:
            logger.warning("No cells found in row")
            return None

        # Si on a déjà un dictionnaire du joueur (depuis un autre tableau), on le copie
        player_data = existing_player_data.copy() if existing_player_data else {
            'name': None,
            'nationality': 'Unknown',
            'position': 'Unknown',
            'age': None,
            'minutes': None,
            'goals': None,
            'assists': None,
            'gk_clean_sheets': None,
            'gk_saves': None,
            'gk_save_pct': None,
            'tackles': None,
            'tackles_won': None,
            'interceptions': None,
            'passes': None,
            'passes_completed': None,
            'through_balls': None,
            'cards_yellow': None,
            'cards_red': None,
            'carries_distance': None,
            'goals_per_shot': None,
            'gk_pens_saved': None,
            'shots': None,
            'aerials_won_pct': None,
            'gk_def_actions_outside_pen_area': None,
            'image': None  # Champ pour l'URL de l'image
        }

        # Extraction du nom du joueur et de l'ID pour l'image
        name_cell = row.find('th', {'data-stat': 'player'}) or row.find('td', {'data-stat': 'player'})
        if name_cell:
            name_link = name_cell.find('a')
            if name_link:
                player_data['name'] = name_link.get_text(strip=True).strip()
                # Extraire l'ID du joueur à partir de l'URL du lien
                href = name_link.get('href', '')
                player_id = href.split('/players/')[1].split('/')[0] if '/players/' in href else None
                if player_id:
                    # Construire l'URL de la headshot avec l'ID et l'année la plus récente (exemple : 2022)
                    player_data['image'] = f"https://fbref.com/req/202302030/images/headshots/{player_id}_2022.jpg"
            else:
                player_data['name'] = name_cell.get_text(strip=True).strip()
        elif column_indices.get('name') is not None and column_indices['name'] < len(cells):
            name_cell = cells[column_indices['name']]
            name_link = name_cell.find('a')
            if name_link:
                player_data['name'] = name_link.get_text(strip=True).strip()
                href = name_link.get('href', '')
                player_id = href.split('/players/')[1].split('/')[0] if '/players/' in href else None
                if player_id:
                    player_data['image'] = f"https://fbref.com/req/202302030/images/headshots/{player_id}_2022.jpg"
            else:
                player_data['name'] = name_cell.get_text(strip=True).strip()

        # Extraction de la nationalité
        if player_data.get('nationality') in [None, 'Unknown']:
            nationality_cell = row.find('td', {'data-stat': 'nationality'})
            if nationality_cell:
                flag_img = nationality_cell.find('img')
                if flag_img and (flag_img.get('alt') or flag_img.get('title')):
                    player_data['nationality'] = self.clean_nationality(flag_img.get('alt') or flag_img.get('title'))
                elif nationality_cell.get_text(strip=True):
                    player_data['nationality'] = self.clean_nationality(nationality_cell.get_text(strip=True))

        # Position
        if not player_data.get('position') or player_data['position'] == 'Unknown':
            position_cell = row.find('td', {'data-stat': 'position'})
            if position_cell and position_cell.get_text(strip=True):
                player_data['position'] = position_cell.get_text(strip=True)

        # Âge
        if not player_data.get('age'):
            age_cell = row.find('td', {'data-stat': 'age'})
            if age_cell and age_cell.get_text(strip=True):
                age_text = age_cell.get_text(strip=True)
                age_parts = age_text.split('-')
                if age_parts and age_parts[0].isdigit():
                    try:
                        player_data['age'] = int(age_parts[0])
                    except ValueError:
                        logger.warning(f"Invalid age format: {age_text}")

        # Extraction des statistiques chiffrées
        stats_mapping = {
            'minutes': 'minutes',
            'goals': 'goals',
            'assists': 'assists',
            'gk_clean_sheets': 'gk_clean_sheets',
            'gk_saves': 'gk_saves',
            'tackles': 'tackles',
            'gk_save_pct': 'gk_save_pct',
            'interceptions': 'interceptions',
            'passes': 'passes',  # Ajouté pour correspondre à "Att" dans Passing
            'passes_completed': 'passes_completed',  # Ajouté pour correspondre à "Cmp" dans Passing
            'pass_completion': 'pass_completion',
            'cards_yellow': 'cards_yellow',
            'cards_red': 'cards_red',
            'through_balls': 'through_balls',
            'tackles_won': 'tackles_won',
            'goals_per_shot': 'goals_per_shot',
            'carries_distance': 'carries_distance',
            'aerials_won_pct': 'aerials_won_pct',
            'gk_pens_saved': 'gk_pens_saved',
            'shots': 'shots',
            'gk_def_actions_outside_pen_area': 'gk_def_actions_outside_pen_area'
        }

        for stat_name, data_key in stats_mapping.items():
            cell = row.find('td', {'data-stat': stat_name})
            if cell:
                value = cell.get_text(strip=True)
                if value and value not in ['-', '']:
                    parsed = self.safe_float(value) if stat_name in ['pass_completion', 'goals_per_shot', 'gk_save_pct', 'aerials_won_pct'] else self.safe_int(value)
                    if parsed is not None and player_data.get(data_key) in [None, '', '-', 0]:
                        player_data[data_key] = parsed

        # Vérification finale : un nom valide est nécessaire
        if (player_data['name'] and
            player_data['name'].strip() and
            not player_data['name'].replace('-', '').isdigit() and
            len(player_data['name'].strip().split()) >= 1 and
            player_data['name'].lower() not in ['player', 'name', 'squad total', 'total', 'bench']):
            return player_data

        logger.warning(f"Invalid or skipped player data: {player_data.get('name')}")
        return None
    def merge_stats_from_all_tables(self, soup):
        """
        Fusionne les statistiques de tous les tableaux disponibles sur une page FBref.
        Retourne une liste de dictionnaires de stats fusionnées par joueur.
        """
        players_dict = {}
        tables = soup.find_all('table', class_='stats_table')  # Filtrer les tables de stats

        for table in tables:
            table_id = table.get('id', 'unknown_table')
            logger.info(f"Traitement de la table : {table_id}")

            thead = table.find('thead')
            tbody = table.find('tbody')

            if not tbody:
                logger.warning(f"Aucun <tbody> trouvé pour la table {table_id}")
                continue

            # Obtenir les en-têtes directement depuis thead
            headers = [th.get('data-stat', th.get_text(strip=True).lower()) for th in thead.find_all('th') if th]
            column_indices = self.find_column_indices(headers)

            for row in tbody.find_all('tr'):
                if 'class' in row.attrs and 'thead' in row.attrs['class']:
                    continue

                extracted_data = self.extract_player_data(row, column_indices)
                if extracted_data and extracted_data.get('name'):
                    name = extracted_data['name']
                    if name in players_dict:
                        players_dict[name] = self.extract_player_data(row, column_indices, existing_player_data=players_dict[name])
                    else:
                        players_dict[name] = extracted_data

        logger.info(f"Fusion terminée : {len(players_dict)} joueurs détectés avec stats multi-tables.")
        return list(players_dict.values())

    def get_players(self, team_url, retries=3):
        for attempt in range(retries):
            try:
                self.driver.get(team_url)
                self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "table, .section_heading")))
                time.sleep(3)

                soup = BeautifulSoup(self.driver.page_source, 'html.parser')
                players = self.merge_stats_from_all_tables(soup)  # Utiliser la fusion multi-tables

                if not players:
                    logger.warning(f"No valid players extracted from {team_url}")
                    debug_filename = f"debug_{team_url.split('/')[-2]}_{attempt}.html"
                    with open(debug_filename, 'w', encoding='utf-8') as f:
                        f.write(str(soup))
                    logger.info(f"Saved debug HTML to {debug_filename}")
                else:
                    logger.info(f"Found {len(players)} valid players for team at {team_url}")
                    if players:
                        logger.info(f"Sample players: {players[:3]}")

                return players

            except TimeoutException:
                logger.error(f"Timeout loading team page: {team_url} (Attempt {attempt+1}/{retries})")
                if attempt < retries - 1:
                    time.sleep(random.uniform(5, 10))
                continue
            except Exception as e:
                logger.error(f"Error scraping players from {team_url}: {e}")
                if attempt < retries - 1:
                    time.sleep(random.uniform(3, 6))
                continue

        logger.error(f"Failed to scrape players from {team_url} after {retries} attempts")
        return []

    def insert_club(self, club):
        conn = self.get_db_connection()
        try:
            with conn.cursor() as cur:
                cur.execute("""
                    INSERT INTO clubs (name, league, level)
                    VALUES (%s, %s, %s)
                    ON CONFLICT (name) DO UPDATE
                    SET league = EXCLUDED.league, level = EXCLUDED.level
                    RETURNING id;
                """, (club['name'], club['league'], club['level']))
                club_id = cur.fetchone()
                conn.commit()
                return club_id[0] if club_id else None
        except Exception as e:
            logger.error(f"Error inserting club {club['name']}: {e}")
            conn.rollback()
            return None
        finally:
            self.release_db_connection(conn)

    def classify_position(self, position):
        """Classifie les positions en catégories principales avec gestion des doubles postes"""
        if not position:
            return 'Unknown'
        
        position = position.upper()
        
        # Définir l'ordre de priorité (du plus spécialisé au plus général)
        priority_order = ['Gardien', 'Attaquant', 'Milieu', 'Defenseur']
        
        # Dictionnaire des catégories avec leurs codes
        position_categories = {
            'Gardien': ['GK'],
            'Defenseur': ['CB', 'RB', 'LB', 'DF', 'WB'],
            'Milieu': ['CM', 'CDM', 'CAM', 'RM', 'LM', 'MF', 'DM', 'AM'],
            'Attaquant': ['ST', 'CF', 'LW', 'RW', 'FW']
        }
        
        # Séparer les positions multiples (par virgule ou autre séparateur)
        positions = [pos.strip() for pos in position.replace(',', '/').replace('-', '/').split('/')]
        
        # Identifier toutes les catégories possibles
        found_categories = []
        for pos in positions:
            for category, codes in position_categories.items():
                if any(code in pos for code in codes):
                    if category not in found_categories:
                        found_categories.append(category)
        
        # Si aucune catégorie trouvée
        if not found_categories:
            return 'Unknown'
        
        # Si une seule catégorie trouvée
        if len(found_categories) == 1:
            return found_categories[0]
        
        # Si plusieurs catégories, appliquer la priorité
        for category in priority_order:
            if category in found_categories:
                return category
        
        return found_categories[0]  # Fallback
    def classify_position_custom_rules(self, position):
        """Classification avec règles spécifiques pour certaines combinaisons"""
        if not position:
            return 'Unknown'
        
        position = position.upper()
        
        # Règles spécifiques pour certaines combinaisons
        position_rules = {
            # Cas spéciaux - ordre important
            ('MF', 'DF'): 'Milieu',  # Comme dans votre exemple
            ('DF', 'MF'): 'Milieu',
            ('CAM', 'ST'): 'Attaquant',
            ('CM', 'CAM'): 'Milieu',
            ('CB', 'CDM'): 'Defenseur',
            ('WB', 'MF'): 'Milieu',  # Wing-back qui joue aussi milieu
        }
        
        # Séparer les positions
        positions = [pos.strip() for pos in position.replace(',', '/').replace('-', '/').split('/')]
        
        # Vérifier les règles spécifiques d'abord
        if len(positions) >= 2:
            pos_tuple = tuple(sorted(positions[:2]))  # Prendre les 2 premières positions
            for rule_positions, category in position_rules.items():
                if all(any(rule_pos in pos for rule_pos in rule_positions) for pos in positions[:2]):
                    return category
        
        # Sinon, utiliser la logique standard
        return self.classify_position_standard(position)

    def classify_position_standard(self, position):
        """Classification standard (votre logique actuelle)"""
        position = position.upper()
        
        # Gardiens (priorité absolue)
        if 'GK' in position:
            return 'Gardien'
        
        # Attaquants (deuxième priorité)
        if any(pos in position for pos in ['ST', 'CF', 'LW', 'RW', 'FW']):
            return 'Attaquant'
        
        # Milieux (troisième priorité)
        if any(pos in position for pos in ['CM', 'CDM', 'CAM', 'RM', 'LM', 'MF', 'DM', 'AM']):
            return 'Milieu'
        
        # Défenseurs (dernière priorité)
        if any(pos in position for pos in ['CB', 'RB', 'LB', 'DF', 'WB']):
            return 'Defenseur'
        
        return 'Unknown'
    def insert_position_stats(self, player_id, player_data, position_category):
        """Insère les statistiques spécifiques à chaque position"""
        conn = self.get_db_connection()
        try:
            with conn.cursor() as cur:
                if position_category == 'Attaquant':
                    taux_conversion = None
                    shots_total = player_data.get('shots')
                    goals = player_data.get('goals')
                    

                    cur.execute("""
                        INSERT INTO attaquants (joueur_id, buts_marques, taux_conversion, passes_decisives, minutes_jouees, shots_total)
                        VALUES (%s, %s, %s, %s, %s, %s)
                        ON CONFLICT (joueur_id) DO UPDATE SET
                            buts_marques = EXCLUDED.buts_marques,
                            taux_conversion = EXCLUDED.taux_conversion,
                            passes_decisives = EXCLUDED.passes_decisives,
                            minutes_jouees = EXCLUDED.minutes_jouees,
                            shots_total = EXCLUDED.shots_total
                    """, (
                        player_id,
                        player_data.get('goals') or 0,
                        player_data.get('goals_per_shot'),
                        player_data.get('assists') or 0,
                        player_data.get('minutes') or 0,
                        shots_total or 0
                    ))
                
                elif position_category == 'Gardien':
                    
                    cur.execute("""
                        INSERT INTO gardiens (joueur_id, pourcentage_arrets, clean_sheets, sorties, penalties_arretes, matchs_joues, saves)
                        VALUES (%s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (joueur_id) DO UPDATE SET
                            pourcentage_arrets = EXCLUDED.pourcentage_arrets,
                            clean_sheets = EXCLUDED.clean_sheets,
                            sorties = EXCLUDED.sorties,
                            penalties_arretes = EXCLUDED.penalties_arretes,
                            matchs_joues = EXCLUDED.matchs_joues,
                            saves = EXCLUDED.saves 
                    """, (
                        player_id,
                        player_data.get('gk_save_pct') or 0.0,  # À ajuster avec une vraie stat gardien si disponible
                        player_data.get('gk_clean_sheets') or 0,
                        player_data.get('gk_def_actions_outside_pen_area') or 0,  # Placeholder, à ajuster
                        player_data.get('gk_pens_saved') or 0,
                        player_data.get('minutes', 0) // 90 if player_data.get('minutes') else 0,
                        player_data.get('gk_saves') or 0
                    ))
                
                elif position_category == 'Defenseur':
                    cur.execute("""
                        INSERT INTO defenseurs (joueur_id, tacles_reussis, interceptions, duels_aeriens, duels_gagnes, clean_sheets, cartons_jaunes, cartons_rouge, minutes_jouees, tackles)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (joueur_id) DO UPDATE SET
                            tacles_reussis = EXCLUDED.tacles_reussis,
                            interceptions = EXCLUDED.interceptions,
                            duels_aeriens = EXCLUDED.duels_aeriens,
                            duels_gagnes = EXCLUDED.duels_gagnes,
                            clean_sheets = EXCLUDED.clean_sheets,
                            cartons_jaunes = EXCLUDED.cartons_jaunes,
                            cartons_rouge = EXCLUDED.cartons_rouge,
                            minutes_jouees = EXCLUDED.minutes_jouees,
                            tackles = EXCLUDED.tackles
                    """, (
                        player_id,
                        player_data.get('tackles_won') or 0,
                        player_data.get('interceptions') or 0,
                        player_data.get('tackles') or 0,  # Placeholder
                        player_data.get('aerials_won_pct') or 0,  # Placeholder
                        player_data.get('clean_sheets') or 0,
                        player_data.get('cards_yellow') or 0,
                        player_data.get('cards_red') or 0,
                        player_data.get('minutes') or 0,
                        player_data.get('tackles') or 0
                    ))
                
                elif position_category == 'Milieu':
                    cur.execute("""
                        INSERT INTO milieux (joueur_id, passes_reussies, recuperations, distance_parcourue, passes_cle, passes_decisives,buts_marques, minutes_jouees)
                        VALUES (%s, %s, %s, %s, %s, %s,%s, %s)
                        ON CONFLICT (joueur_id) DO UPDATE SET
                            passes_reussies = EXCLUDED.passes_reussies,
                            recuperations = EXCLUDED.recuperations,
                            distance_parcourue = EXCLUDED.distance_parcourue,
                            passes_cle = EXCLUDED.passes_cle,
                            passes_decisives = EXCLUDED.passes_decisives,
                            buts_marques = EXCLUDED.buts_marques,
                            minutes_jouees = EXCLUDED.minutes_jouees
                    """, (
                        player_id,
                        player_data.get('passes_completed') or 0,
                        player_data.get('interceptions') or 0,
                        player_data.get('carries_distance') or 0.0,  # Placeholder
                        player_data.get('through_balls') or 0.0,
                        player_data.get('assists') or 0,
                        player_data.get('goals') or 0,
                        player_data.get('minutes') or 0
                    ))
            
            conn.commit()
        
        except Exception as e:
            logger.error(f"Error inserting position stats for player {player_id}: {e}")
            conn.rollback()
        finally:
            self.release_db_connection(conn)

    def insert_players_batch(self, players_data, club_id):
        """Insertion par lots pour de meilleures performances avec statistiques par position"""
        if not players_data:
            return
        
        conn = self.get_db_connection()
        try:
            with conn.cursor() as cur:
                # Vérifier quels joueurs existent déjà
                existing_players = {}
                if players_data:
                    player_names = [player['name'] for player in players_data]
                    format_strings = ','.join(['%s'] * len(player_names))
                    cur.execute(f"""
                        SELECT name, id FROM players 
                        WHERE club_id = %s AND name IN ({format_strings})
                    """, [club_id] + player_names)
                    existing_players = {row[0]: row[1] for row in cur.fetchall()}
                
                # Séparer les nouveaux joueurs et les joueurs existants
                new_players = []
                existing_player_data = []
                
                for player in players_data:
                    if player['name'] not in existing_players:
                        new_players.append(player)
                    else:
                        player_id = existing_players[player['name']]
                        existing_player_data.append((player_id, player))
                
                # Insérer les nouveaux joueurs
                if new_players:
                    data_to_insert = [
                        (player['name'], player['age'], player['position'], player['nationality'], club_id, player['image'])
                        for player in new_players
                    ]
                    
                    execute_batch(
                        cur,
                        """
                        INSERT INTO players (name, age, position, nationality, club_id, image)
                        VALUES (%s, %s, %s, %s, %s, %s)
                        ON CONFLICT ON CONSTRAINT players_name_club_unique DO NOTHING
                        """,
                        data_to_insert,
                        page_size=100
                    )
                    
                    # Récupérer les IDs des nouveaux joueurs
                    cur.execute("""
                        SELECT id, name FROM players 
                        WHERE club_id = %s AND name = ANY(%s)
                    """, (club_id, [player['name'] for player in new_players]))
                    
                    new_player_ids = {row[1]: row[0] for row in cur.fetchall()}
                    
                    # Ajouter les statistiques pour les nouveaux joueurs
                    for player in new_players:
                        if player['name'] in new_player_ids:
                            player_id = new_player_ids[player['name']]
                            position_category = self.classify_position(player['position'])
                            if position_category != 'Unknown':
                                self.insert_position_stats(player_id, player, position_category)
                    
                    logger.info(f"Inserted {len(new_players)} new players with statistics")
                
                # Mettre à jour les statistiques des joueurs existants
                for player_id, player_data in existing_player_data:
                    position_category = self.classify_position(player_data['position'])
                    if position_category != 'Unknown':
                        self.insert_position_stats(player_id, player_data, position_category)
                
                if existing_player_data:
                    logger.info(f"Updated statistics for {len(existing_player_data)} existing players")
                
                conn.commit()
                
        except Exception as e:
            logger.error(f"Error inserting players batch: {e}")
            conn.rollback()
        finally:
            self.release_db_connection(conn)

    def scrape_league(self, league_url, league_name):
        teams = self.get_teams(league_url)
        for team in teams:
            logger.info(f"Processing team: {team['name']}")
            club_data = {'name': team['name'], 'league': league_name, 'level': 'Professional'}
            club_id = self.insert_club(club_data)
            if club_id:
                players = self.get_players(team['url'])
                if players:
                    self.insert_players_batch(players, club_id)
                time.sleep(random.uniform(5, 10))  # Délai plus long pour éviter les blocages
            else:
                logger.error(f"Failed to insert club: {team['name']}")

    def get_player_stats_by_position(self, position_category, limit=None):
        """Récupère les statistiques des joueurs par position"""
        conn = self.get_db_connection()
        try:
            with conn.cursor() as cur:
                if position_category == 'Gardien':
                    query = """
                        SELECT p.name, p.age, p.nationality, c.name as club, c.league,
                               g.pourcentage_arrets, g.clean_sheets, g.sorties, g.penalties_arretes, g.matchs_joues, g.saves
                        FROM players p
                        JOIN clubs c ON p.club_id = c.id
                        JOIN gardiens g ON p.id = g.joueur_id
                        ORDER BY g.clean_sheets DESC, g.pourcentage_arrets DESC
                    """
                elif position_category == 'Defenseur':
                    query = """
                        SELECT p.name, p.age, p.nationality, c.name as club, c.league,
                               d.tacles_reussis, d.interceptions, d.duels_aeriens, d.duels_gagnes, 
                               d.clean_sheets, d.cartons_jaunes, d.cartons_rouge, d.minutes_jouees, d.tackles
                        FROM players p
                        JOIN clubs c ON p.club_id = c.id
                        JOIN defenseurs d ON p.id = d.joueur_id
                        ORDER BY d.tacles_reussis DESC, d.interceptions DESC
                    """
                elif position_category == 'Milieu':
                    query = """
                        SELECT p.name, p.age, p.nationality, c.name as club, c.league,
                               m.passes_reussies, m.recuperations, m.distance_parcourue, 
                               m.passes_cle, m.passes_decisives, m.minutes_jouees
                        FROM players p
                        JOIN clubs c ON p.club_id = c.id
                        JOIN milieux m ON p.id = m.joueur_id
                        ORDER BY m.passes_reussies DESC, m.passes_decisives DESC
                    """
                elif position_category == 'Attaquant':
                    query = """
                        SELECT p.name, p.age, p.nationality, c.name as club, c.league,
                               a.buts_marques, a.taux_conversion, a.passes_decisives, a.minutes_jouees, a.shots_total
                        FROM players p
                        JOIN clubs c ON p.club_id = c.id
                        JOIN attaquants a ON p.id = a.joueur_id
                        ORDER BY a.buts_marques DESC, a.passes_decisives DESC
                    """
                else:
                    return []
                
                if limit:
                    query += f" LIMIT {limit}"
                
                cur.execute(query)
                return cur.fetchall()
                
        except Exception as e:
            logger.error(f"Error getting player stats by position: {e}")
            return []
        finally:
            self.release_db_connection(conn)

    def get_team_stats_summary(self, club_name):
        """Récupère un résumé des statistiques d'équipe"""
        conn = self.get_db_connection()
        try:
            with conn.cursor() as cur:
                # Statistiques générales de l'équipe
                cur.execute("""
                    SELECT 
                        COUNT(*) as total_players,
                        AVG(age) as avg_age,
                        COUNT(CASE WHEN position LIKE '%GK%' THEN 1 END) as goalkeepers,
                        COUNT(CASE WHEN position LIKE '%CB%' OR position LIKE '%RB%' OR position LIKE '%LB%' THEN 1 END) as defenders,
                        COUNT(CASE WHEN position LIKE '%CM%' OR position LIKE '%CDM%' OR position LIKE '%CAM%' THEN 1 END) as midfielders,
                        COUNT(CASE WHEN position LIKE '%ST%' OR position LIKE '%CF%' OR position LIKE '%LW%' OR position LIKE '%RW%' THEN 1 END) as attackers
                    FROM players p
                    JOIN clubs c ON p.club_id = c.id
                    WHERE c.name = %s
                """, (club_name,))
                
                team_stats = cur.fetchone()
                
                # Top scorers de l'équipe
                cur.execute("""
                    SELECT p.name, a.buts_marques, a.passes_decisives
                    FROM players p
                    JOIN clubs c ON p.club_id = c.id
                    JOIN attaquants a ON p.id = a.joueur_id
                    WHERE c.name = %s AND a.buts_marques > 0
                    ORDER BY a.buts_marques DESC
                    LIMIT 5
                """, (club_name,))
                
                top_scorers = cur.fetchall()
                
                return {
                    'team_stats': team_stats,
                    'top_scorers': top_scorers
                }
                
        except Exception as e:
            logger.error(f"Error getting team stats summary: {e}")
            return None
        finally:
            self.release_db_connection(conn)

    def close(self):
        try:
            if self.driver:
                self.driver.quit()
                logger.info("Chrome driver closed successfully")
            if self.db_pool:
                self.db_pool.closeall()
                logger.info("Database connection pool closed")
        except Exception as e:
            logger.error(f"Error during cleanup: {e}")

def main():
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'database': os.getenv('DB_NAME', 'football_db'),
        'user': os.getenv('DB_USER', 'postgres'),
        'password': os.getenv('DB_PASSWORD', 'adminadmin'),
        'port': os.getenv('DB_PORT', 5432)
    }

    scraper = None
    try:
        scraper = FBrefScraperSelenium(db_config)
        scraper.create_tables()

        leagues = {
            'Premier League': 'https://fbref.com/en/comps/9/2024-2025/2024-2025-Premier-League-Stats',
            'La Liga': 'https://fbref.com/en/comps/12/2024-2025/2024-2025-La-Liga-Stats',
            'Serie A': 'https://fbref.com/en/comps/11/2024-2025/2024-2025-Serie-A-Stats',
            'Bundesliga': 'https://fbref.com/en/comps/20/2024-2025/2024-2025-Bundesliga-Stats',
            'Ligue 1': 'https://fbref.com/en/comps/13/2024-2025/2024-2025-Ligue-1-Stats',
        }

        for league_name, league_url in leagues.items():
            logger.info(f"Scraping {league_name}")
            scraper.scrape_league(league_url, league_name)
            time.sleep(random.uniform(10, 20))

        # Exemple d'utilisation des nouvelles fonctionnalités
        logger.info("=== EXEMPLES DE STATISTIQUES ===")
        
        # Top 10 gardiens
        top_goalkeepers = scraper.get_player_stats_by_position('Gardien', limit=10)
        logger.info(f"Top 10 gardiens: {len(top_goalkeepers)} trouvés")
        
        # Top 10 attaquants
        top_attackers = scraper.get_player_stats_by_position('Attaquant', limit=10)
        logger.info(f"Top 10 attaquants: {len(top_attackers)} trouvés")
        
        # Statistiques d'une équipe spécifique (exemple)
        team_stats = scraper.get_team_stats_summary('Manchester City')
        if team_stats and team_stats['team_stats']:
            logger.info(f"Statistiques Manchester City: {team_stats['team_stats']}")
        else:
            logger.warning("No data available for Manchester City")

    except Exception as e:
        logger.error(f"Error in main execution: {e}")
        raise
    finally:
        if scraper:
            scraper.close()

if __name__ == '__main__':
    main()