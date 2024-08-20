docker run -id --name=shorturl-web \
--network shorturl-network \
--network-alias shorturl-web \
--ip 192.168.1.10 \
-p 80:80 \
-v /d/data/projects/lozhu-shorturl/shorturl-web/nginx/nginx.conf:/etc/nginx/nginx.conf \
-v /d/data/projects/lozhu-shorturl/shorturl-web/nginx/conf.d:/etc/nginx/conf.d \
-v /d/data/projects/lozhu-shorturl/shorturl-web/nginx/log:/var/log/nginx \
shorturl-web:v1.1.0
