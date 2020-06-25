echo $"stop proxy container ..."
docker stop proxy

echo $"remove proxy container ..."
docker rm proxy

echo $"remove proxy image ..."
docker rmi proxy

echo $"build proxy image ..."
cd /opt/baichuan/proxy
docker build -t proxy .

echo $"run proxy container ..."
docker run -d -p 9005:9005 --name proxy proxy

docker push proxy