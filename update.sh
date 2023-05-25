#!/bin/bash
echo "Start Updating"
URL="https://github.com/islandsexit/Park/raw/final/RAS_CAM.zip" 
NAME_UPDATE_ZIP=update.zip
NAME_PROJECT=RAS_CAM
NAME_PROJECT_BP=RAS_CAM_BACKUP

rm -rf $NAME_PROJECT_BP

mv ./$NAME_PROJECT ./$NAME_PROJECT_BP

{
wget -O $NAME_UPDATE_ZIP $URL 
unzip -q $NAME_UPDATE_ZIP -d ./$NAME_PROJECT
rm $NAME_UPDATE_ZIP
}||{
    echo "EXCEPTION in downloading update"
    mv ./$NAME_PROJECT_BP ./$NAME_PROJECT 
}
