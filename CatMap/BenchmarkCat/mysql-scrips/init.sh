sudo docker run -d -e MYSQL_ROOT_PASSWORD=123 -p 3306:3306 mysql --secure-file-priv='' 



echo "start,end,worker_id,source,mapping.name,exportID,records.size,message.count,record.id,rdf.file.size" > CSV/nl.uva.sne.vre4eic.cat_exporter.Worker.csv
for f in 'CSV/nl.uva.sne.vre4eic.cat_exporter.Worker$1'*.csv; do
    sed -e 1d $f >> CSV/nl.uva.sne.vre4eic.cat_exporter.Worker.csv
done

for f in CSV/*.csv; do
    fileName=$(basename $f)
    sudo docker cp $f $(sudo docker ps | grep mysql | awk '{print $1}'):/tmp/$fileName
done

for f in *.sql; do
    fileName=$(basename $f)
    echo $fileName
    sudo docker cp $f $(sudo docker ps | grep mysql | awk '{print $1}'):/$fileName
done

sleep 10
sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 -s -N -e "create database metrics;"'





sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c "mysql -uroot -p123 metrics < init.sql" 

sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics < execution_time.sql' > CSV/exec_time.tsv

sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics < speedup_efficiency.sql' > CSV/speedup_efficiency.tsv

for i in {1..16}
do
    sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c "echo \"SET @consumers =$i;\" > gnatt$i.sql"
    sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c "cat gantt.sql >>  gnatt$i.sql"
    sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c "mysql -uroot -p123 --database=metrics < gnatt$i.sql" > CSV/gnatt$i.tsv
done






# sudo docker stop  $(sudo docker ps | grep mysql | awk '{print $1}')
