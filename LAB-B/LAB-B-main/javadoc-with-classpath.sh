#!/bin/bash

# Crea la directory di output se non esiste
mkdir -p docs/javadoc

# Sposta temporaneamente il file module-info.java
mv src/main/java/module-info.java module-info.java.bak

# Trova tutte le librerie JAR nel repository locale Maven
MVN_REPO=~/.m2/repository
CLASSPATH=""

# Cerca le librerie JavaFX
for JAR in $(find "$MVN_REPO/org/openjfx" -name "*.jar"); do
  if [ -z "$CLASSPATH" ]; then
    CLASSPATH="$JAR"
  else
    CLASSPATH="$CLASSPATH:$JAR"
  fi
done

# Cerca altre librerie che potrebbero essere necessarie
for LIB in "org.controlsfx" "com.dlsc.formsfx" "net.synedra.validatorfx"; do
  for JAR in $(find "$MVN_REPO" -path "*/$LIB/*" -name "*.jar"); do
    CLASSPATH="$CLASSPATH:$JAR"
  done
done

# Genera la Javadoc con il classpath esteso
javadoc -d docs/javadoc \
        -sourcepath src/main/java \
        -classpath "$CLASSPATH" \
        -encoding UTF-8 \
        -charset UTF-8 \
        -docencoding UTF-8 \
        -Xdoclint:none \
        -quiet \
        -windowtitle "Book Recommender API" \
        -doctitle "<h1>Book Recommender System - Documentazione API</h1>" \
        -header "<strong>Book Recommender</strong>" \
        -bottom "<p>&copy; 2025 Book Recommender System. Tutti i diritti riservati.</p>" \
        book_recommender.lab_b

# Ripristina il file module-info.java
mv module-info.java.bak src/main/java/module-info.java

echo "Documentazione Javadoc generata in docs/javadoc/"