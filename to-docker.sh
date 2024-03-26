#!/usr/bin/env bash

# 需要在项目中的 jib plugin 中添加以下配置
# <executions>
#     <execution>
#         <phase>package</phase>
#         <goals><goal>build</goal></goals>
#     </execution>
# </executions>

project="$1"
if [ -z "${project}" ]; then
    echo "usage:"
    echo "  「$(readlink -f $0) 项目名」  推送项目到 docker 服务器"
    exit 1
fi

# 用「/项目名-所在分支:最新的 commit-hash」做为 docker 服务器的 url 地址
now="$(date '+%Y-%m-%d')"
branch="$(git rev-parse --abbrev-ref HEAD)"
latest_commit="$(git log --format=format:'%h' | head -n 1)"

# 配置项
docker_url="ip:port"
docker_user="xx"
docker_pass="xxx"

docker_image="${docker_url}/${project}-${branch}:${now}-${latest_commit}"

# 如果加了上面的 executions 配置, 则下面的命令可以去掉 jib:build 选项
mvn clean package -DskipTests -pl ${project} -am \
    -DsendCredentialsOverHttp=true \
    -Djib.to.image=${docker_image} \
    -Djib.to.auth.username=${docker_user} \
    -Djib.to.auth.password=${docker_pass} jib:build

echo "使用 docker pull ${docker_image} 命令下载最新镜像"
