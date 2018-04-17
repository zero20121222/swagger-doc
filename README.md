# swagger-doc
Convert swagger v2 api to asciidoctor to html

#### create jar
mvn clean package -Dmaven.test.skip

#### get tips
java -jar target/swagger-doc-1.0-SNAPSHOT.jar -h

#### create swagger api document with html(-s: swagger api address)
java -jar target/jiddN-swagger-1.0-SNAPSHOT.jar -s http://127.0.0.1:8093/v2/api-docs  
