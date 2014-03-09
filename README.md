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

1. Create a new project:
  1. File menu -> New Project
    * Choose: Import project from external model (maven) and use default values.

2. Adjust settings:
  1. IntelliJ menu -> Preferences... -> Project Settings -> Compiler -> Scala Compiler
    * Under Project FSC: select Compiler library

  2. File menu -> Project Structure... -> Facets
    * Under Compiler instantiation: use project FSC

3. After building, to run:
  1. Debug: src/main/scala/com/laialfa/Main.scala
