#!/usr/bin/env bash

# 用「/项目名-所在分支:最新的 commit-hash」做为 docker 服务器的 url 地址
now="$(date '+%Y-%m-%d')"
branch="$(git rev-parse --abbrev-ref HEAD)"
latest_commit="$(git log --format=format:'%h' | head -n 1)"

docker_image="ip:port/web-backend-${branch}:${now}-${latest_commit}"
docker_user="xx"
docker_pass="xxx"

mvn clean package -DskipTests -pl "web-backend" -am \
    -DsendCredentialsOverHttp=true \
    -Djib.to.image=${docker_image} \
    -Djib.to.auth.username=${docker_user} \
    -Djib.to.auth.password=${docker_pass}
