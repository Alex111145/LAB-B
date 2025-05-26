#!/bin/bash

# Determina la directory in cui si trova lo script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Calcola la directory principale del progetto (un livello superiore rispetto a bin)
PROJECT_DIR="$( cd "$SCRIPT_DIR/.." >/dev/null 2>&1 && pwd )"

# Vai alla directory del progetto
cd "$PROJECT_DIR"

# Mostra informazioni
echo "Directory dello script: $SCRIPT_DIR"
echo "Directory del progetto: $PROJECT_DIR"
echo "Avvio del client con Maven ..."

# Avvia il client usando Maven
# Imposta le opzioni Java direttamente nella riga di comando di Maven
mvn clean javafx:run \
  -e \
  -Djavafx.mainClass=book_recommender.lab_b.Client \
  -Dprism.order=sw \
  -Dmonocle.platform=Headless \
  -Dprism.verbose=true \
  -Djavafx.verbose=true