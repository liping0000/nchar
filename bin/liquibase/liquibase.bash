
# latest: http://www.liquibase.org/manual/generating_changelogs
# command for diff creation:

./liquibase \
 --logLevel debug \
 --classpath=./lib/ojdbc6.jar \
 --driver=oracle.jdbc.OracleDriver \
 --changeLogFile=/tmp/db.changelog.xml \
 --username=CAMS \
 --password=xxxxxx \
 --url=jdbc:oracle:thin:@localhost:1521:XE \
 diffChangeLog \
 --referenceUsername=CHARMS \
 --referencePassword=xxxxxx \
 --referenceUrl=jdbc:oracle:thin:@localhost:1521:XE \

# command for init create log:

./liquibase \
 --logLevel debug \
 --classpath=./lib/ojdbc6.jar \
 --driver=oracle.jdbc.OracleDriver \
 --changeLogFile=/tmp/db.changelog.xml \
 --username=CHARMS \
 --password=s3cr37 \
 --url=jdbc:oracle:thin:@localhost:1521:XE \
 generateChangeLog \



