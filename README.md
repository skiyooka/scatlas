scatlas
=======

Scala texture atlas/sprite sheet (for signed distance fields) suitable for OpenGL

Maven
-----

To build and run from the command-line using mvn:

    mvn clean compile
    mvn exec:exec


IntelliJ
--------

File -> New Project

Choose: Import project from external model (maven) and use default values.


Adjust settings:

IntelliJ -> Preferences... -> Project Settings -> Compiler -> Scala Compiler
-Project FSC: select Compiler library

File -> Project Structure... -> Facets
-Compiler instantiation: use project FSC

Debug: /src/main/scala/com/laialfa/Main

