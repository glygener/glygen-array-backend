#!/bin/bash
CURRENT=${PWD}
NOW=$(date +"%d%m%Y")
cd $HOME/glygen-array/virtuoso/backups/
for FILENAME in backup*.bp
do
    mv "${FILENAME}" "${NOW}_${FILENAME}" 2>/dev/null
done
cd "${CURRENT}" 
docker exec -i virtuoso_virtuoso_1 isql-v -U dba -P $DBA_PASSWORD <<EOF
exec('checkpoint');
backup_context_clear();
backup_online('backup_',30000,0,vector('backups'));
exit;
EOF
cp $HOME/glygen-array/virtuoso/backups/backup_*.bp "$HOME/backup/${FILENAME}" 2>/dev/null

