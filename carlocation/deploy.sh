#!/bin/bash

# ===========================================
#  Script de déploiement - CarLocation Backoffice
# ===========================================

set -e  # Arrêter en cas d'erreur

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_NAME="carlocation-bo.jar"
PORT="${PORT:-8080}"

# Fonction d'affichage
print_header() {
    echo -e "${BLUE}"
    echo "==========================================="
    echo "  CarLocation Backoffice - Déploiement"
    echo "==========================================="
    echo -e "${NC}"
}

print_step() {
    echo -e "${YELLOW}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Fonction pour afficher l'aide
show_help() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  build       Compiler le projet uniquement"
    echo "  run         Lancer l'application (compile si nécessaire)"
    echo "  clean       Nettoyer les fichiers compilés"
    echo "  package     Créer le JAR exécutable"
    echo "  deploy      Compiler et lancer l'application"
    echo "  stop        Arrêter l'application en cours"
    echo "  help        Afficher cette aide"
    echo ""
    echo "Variables d'environnement:"
    echo "  PORT        Port du serveur (défaut: 8080)"
    echo ""
    echo "Exemples:"
    echo "  $0 deploy           # Compiler et lancer"
    echo "  PORT=9090 $0 run    # Lancer sur le port 9090"
}

# Fonction pour compiler le framework ririnina d'abord
build_framework() {
    print_step "Compilation du framework ririnina..."
    cd "$PROJECT_DIR/../ririnina"
    mvn clean install -DskipTests -q
    print_success "Framework ririnina compilé"
    cd "$PROJECT_DIR"
}

# Fonction pour compiler le projet
build_project() {
    print_step "Compilation du projet carlocation..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q
    print_success "Projet compilé avec succès"
}

# Fonction pour nettoyer
clean_project() {
    print_step "Nettoyage du projet..."
    cd "$PROJECT_DIR"
    mvn clean -q
    print_success "Projet nettoyé"
}

# Fonction pour arrêter l'application
stop_app() {
    print_step "Recherche des processus en cours..."
    
    # Trouver et tuer les processus Java liés au projet
    PIDS=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)
    
    if [ -n "$PIDS" ]; then
        echo "Arrêt des processus: $PIDS"
        kill $PIDS 2>/dev/null || true
        sleep 2
        # Force kill si toujours en cours
        kill -9 $PIDS 2>/dev/null || true
        print_success "Application arrêtée"
    else
        echo "Aucune application en cours d'exécution"
    fi
}

# Fonction pour lancer l'application
run_app() {
    print_step "Lancement de l'application sur le port $PORT..."
    
    JAR_PATH="$PROJECT_DIR/target/$JAR_NAME"
    
    if [ ! -f "$JAR_PATH" ]; then
        print_error "JAR non trouvé: $JAR_PATH"
        print_step "Compilation en cours..."
        build_project
    fi
    
    echo ""
    echo -e "${GREEN}==========================================="
    echo "  Serveur démarré sur http://localhost:$PORT"
    echo "  Appuyez sur Ctrl+C pour arrêter"
    echo -e "===========================================${NC}"
    echo ""
    
    PORT=$PORT java -jar "$JAR_PATH"
}

# Fonction de déploiement complet
deploy() {
    print_header
    
    # Vérifier si on doit compiler le framework
    if [ "$1" == "--with-framework" ] || [ ! -f "$PROJECT_DIR/../ririnina/target/ririnina-1.0-SNAPSHOT.jar" ]; then
        build_framework
    fi
    
    build_project
    run_app
}

# Main
case "${1:-deploy}" in
    build)
        print_header
        build_project
        ;;
    run)
        print_header
        run_app
        ;;
    clean)
        print_header
        clean_project
        ;;
    package)
        print_header
        build_project
        print_success "JAR créé: target/$JAR_NAME"
        ;;
    deploy)
        deploy "$2"
        ;;
    stop)
        print_header
        stop_app
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Option inconnue: $1"
        show_help
        exit 1
        ;;
esac