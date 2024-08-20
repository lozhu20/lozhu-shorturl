docker run -id \
--name=shorturl-service1 \
--network shorturl-network \
--network-alias shorturl-service1 \
--ip 192.168.1.20 \
-p 8000:8000 \
-e dataSource1Url=jdbc:postgresql://192.168.1.40:5432/shorturldb \
-e dataSource2Url=jdbc:postgresql://192.168.1.41:5432/shorturldb \
-e dataSource3Url=jdbc:postgresql://192.168.1.42:5432/shorturldb \
-e dataSourceUsername=postgres \
-e dataSourcePassword=soymilk.No1! \
-e sysConfigDomain=https://t.happy365.day/ \
shorturl-service-image:v1.1.0
