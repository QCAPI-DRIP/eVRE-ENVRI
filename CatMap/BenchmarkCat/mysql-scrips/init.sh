sudo docker run -d -e MYSQL_ROOT_PASSWORD=123 -p 3306:3306 mysql --secure-file-priv='' 



echo "start,end,worker_id,source,mapping.name,exportID,records.size,message.count,record.id,rdf.file.size" > nl.uva.sne.vre4eic.cat_exporter.Worker.csv
for f in 'nl.uva.sne.vre4eic.cat_exporter.Worker$1'*.csv; do
    sed -e 1d $f >> nl.uva.sne.vre4eic.cat_exporter.Worker.csv
done

sleep 15
sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 -s -N -e "create database metrics;"'



for f in *.csv; do
  sudo docker cp $f $(sudo docker ps | grep mysql | awk '{print $1}'):/tmp/$f
done

for f in *.sql; do
    sudo docker cp $f $(sudo docker ps | grep mysql | awk '{print $1}'):/$f
done

sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c "mysql -uroot -p123 metrics < init.sql" 


sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics -s -N -e  "SELECT * FROM main_benchmark;"'

sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics -s -N -e "SELECT * FROM service_benchmark;"'
   
sudo docker exec -it $(sudo docker ps | grep mysql | awk '{print $1}') sh -c 'mysql -uroot -p123 --database=metrics -s -N -e "SELECT * FROM worker_benchmark;"'   



# sudo docker stop  $(sudo docker ps | grep mysql | awk '{print $1}')
