cd "../docker/"
docker-compose down
docker-compose -f docker-compose-transform-tool.yml up -d
cd "../service/"
./gradlew interoperabilityTestingToolJsonToXml