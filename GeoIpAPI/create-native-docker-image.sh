
../gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false -Dquarkus.native.container-build=true -x test &&

docker build -f src/main/docker/Dockerfile.native-micro -t docker.dankito.net/dankito/geo-ip . &&

docker push docker.dankito.net/dankito/geo-ip