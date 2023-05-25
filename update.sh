#!/bin/bash
echo "Start Updating"
URL="http://www.vim.org/scripts/download_script.php?src_id=11834" 
NAME_UPDATE_ZIP=update.zip
NAME_PROJECT=RAS_CAM
NAME_PROJECT_BP=RAS_CAM_BACKUP

mv ./$NAME_PROJECT ./$NAME_PROJECT_BP

{
wget  $URL -O $NAME_UPDATE_ZIP
unzip -q $NAME_UPDATE_ZIP -d ./$NAME_PROJECT
rm $NAME_UPDATE_ZIP
}||{
    echo "EXCEPTION in downloading update"
    mv ./$NAME_PROJECT_BP ./$NAME_PROJECT 
}
