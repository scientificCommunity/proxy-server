#指定构建所依赖的镜像
FROM hub.c.163.com/library/java:8-alpine

MAINTAINER kunTang

ADD ./*.jar railway.jar

EXPOSE 9004

ENTRYPOINT ["java","-jar","-Xms256m","-Xmx512m","-XX:+HeapDumpOnOutOfMemoryError","-XX:HeapDumpPath=/data/dump/railway/jvm.dump","-XX:+PrintGCDateStamps","-XX:+PrintGCDetails","-Xloggc:/data/log/railway/gc.log","-Dcom.sun.management.jmxremote","-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.port=19999","/railway.jar",">/dev/null 2>&1 &"]