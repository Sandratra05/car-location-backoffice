
cd /home/zark/Bureau/ITU/Annee-3/Framework/Framework && 
javac -parameters -d bin -cp "lib/*" src/*.java src/mg/ririnina/utils/*.java src/mg/ririnina/annotations/*.java src/mg/ririnina/view/*.java
cd bin && 
jar cvf ririnina.jar . && 
cp ririnina.jar /home/zark/Bureau/ITU/Annee-3/Framework/Test/lib/
