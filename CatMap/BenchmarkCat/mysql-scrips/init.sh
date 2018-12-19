sudo docker run -d -e MYSQL_ROOT_PASSWORD=123 -p 3306:3306 mysql --secure-file-priv='' 



echo "start,end,worker_id,source,mapping.name,exportID,records.size,message.count,record.id,rdf.file.size" > CSV/nl.uva.sne.vre4eic.cat_exporter.Worker.csv
for f in 'CSV/nl.uva.sne.vre4eic.cat_exporter.Worker$1'*.csv; do
    sed -e 1d $f >> CSV/nl.uva.sne.vre4eic.cat_exporter.Worker.csv
done

sleep 15
sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 -s -N -e "create database metrics;"'



for f in CSV/*.csv; do
    fileName=$(basename $f)
    sudo docker cp $f $(sudo docker ps | grep mysql | awk '{print $1}'):/tmp/$fileName
done

for f in *.sql; do
    fileName=$(basename $f)
    sudo docker cp $f $(sudo docker ps | grep mysql | awk '{print $1}'):/$fileName
done

sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c "mysql -uroot -p123 metrics < init.sql" 


sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics < exec_time.sql' > CSV/exec_time.tsv

sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics < speedup_efficiency.sql' > CSV/speedup_efficiency.tsv


sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics -s -N -e "SET @ex_id = 7a6cc96e-91b2-4a16-ab3c-38757d96b44f;"' > CSV/gentt.tsv




# sudo docker stop  $(sudo docker ps | grep mysql | awk '{print $1}')
