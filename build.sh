#!/usr/bin/env bash
set -e
if ! command -v javac >/dev/null 2>&1; then
  echo "Java Development Kit (JDK) not found. Install a JDK first (for example Temurin 26)."
  exit 1
fi

echo "Compiling..."
rm -rf out
mkdir out
javac -encoding UTF-8 -d out src/mingeriacc/*.java

echo "Packaging Mingeriacc.jar..."
echo "Main-Class: mingeriacc.Main" > out/manifest.txt
(cd out && jar cfm ../Mingeriacc.jar manifest.txt mingeriacc/*.class)

echo "Done! Mingeriacc.jar has been updated."
