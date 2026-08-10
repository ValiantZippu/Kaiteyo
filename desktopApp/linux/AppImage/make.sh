#!/bin/bash

APP_IMAGE_DIR="desktopApp/linux/AppImage/"

if [ "$(basename "$PWD")" == "AppImage" ]; then
  echo "Running from AppImage folder"
  cd ../../../
elif [ -f "README.md" ]; then
  echo "Running from the repo's root"
else
  echo "Executed from an unsupported directory, aborting"
  exit
fi

./gradlew :desktopApp:createDistributable

# AppDir structure according to https://github.com/AppImage/AppImageKit/wiki/AppDir
cp -r "desktopApp/build/compose/binaries/main/app/Kaiteyo/" "$APP_IMAGE_DIR/AppDir/usr"
cp "$APP_IMAGE_DIR/AppDir/usr/lib/Kaiteyo.png" "$APP_IMAGE_DIR/AppDir/Kaiteyo.png"

cd $APP_IMAGE_DIR

if [ ! -f "appimagetool-x86_64.AppImage" ]; then
  wget https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
fi

chmod +x ./appimagetool-x86_64.AppImage
chmod +x AppDir/AppRun

./appimagetool-x86_64.AppImage AppDir/
chmod +x Kaiteyo-x86_64.AppImage
