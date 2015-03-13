# How to add dependencies in your local Maven repository #

This project uses MYID3 Java library for parsing MP3 tags. Unfortenately there's no Maven repository which serves its artifacts. To install it in your local Maven repository checkout the source and issue these two commands:
```
mvn install:install-file -Dfile=pathToTheProject/skeedeye/lib/myid3.jar -DgroupId=org.cmc.music -DartifactId=sharedlib -Dversion=1 -Dpackaging=jar -DgeneratePom=true

mvn install:install-file -Dfile=pathToTheProject/skeedeye/lib/sharedlib.jar -DgroupId=org.cmc.common -DartifactId=sharedlib -Dversion=1 -Dpackaging=jar -DgeneratePom=true
```