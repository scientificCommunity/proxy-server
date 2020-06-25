#!/usr/bin/env bash
echo $"server is building ..."

mvn clean package -Dmaven.test.skip=true -U

#dev=root@myenv
dev=root@dev

echo $"copying proxy.jar to dev constant ..."

scp ./build/proxy.jar $dev:/opt/baichuan/proxy/
#scp ./bash/deploy.sh $dev:/opt/baichuan/proxy/
scp ./Dockerfile $dev:/opt/baichuan/proxy/

ssh $dev "sh /opt/baichuan/proxy/deploy.sh"
echo $"stop proxy container ..."
ssh $dev "docker stop proxy"

echo $"remove proxy container ..."
ssh $dev "docker rm proxy"

echo $"remove proxy image ..."
ssh $dev "docker rmi proxy"

echo $"build proxy image ..."
ssh $dev "cd /opt/baichuan/proxy;docker build -t proxy ."

echo $"run proxy container ..."

#直接启动
ssh $dev "java -jar -Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/dump/proxy/jvm.dump -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:/data/log/proxy/gc.log -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=19999 /opt/baichuan/proxy/proxy.jar >/dev/null 2>&1 &"

#docker启动
#ssh $dev "docker run -d -p 9004:9004 -p 19999:19999 --name proxy -v /data/log/proxy:/data/log/proxy -v /data/captcha:/data/captcha -v /data/lessons:/data/lessons -v /data/dump/proxy:/data/dump/proxy -v /data/videos:/data/videos -v /fuye/files/:/fuye/files/ --privileged=true proxy"