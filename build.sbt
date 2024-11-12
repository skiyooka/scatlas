enablePlugins(JlinkPlugin) // sbt-native-packager
enablePlugins(JavaAppPackaging) // sbt-native-packager

lazy val root = project.in(file("."))
  .settings(
    name := "scatlas",

    scalaVersion := "3.3.3",
    scalacOptions ++= Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    ),

    // allow run within the sbt shell
    run / fork := true,

    jlinkModules := Seq("java.base",  // set explicitly instead of jdeps
      "java.xml",                     // for scala.xml
      "java.naming",                  // for logback-classic
      "jdk.unsupported",              // for java.nio
      "java.desktop",                 // for javax.swing
    ),
    // to debug run jdeps on output dir or .jar file (e.g. jdeps target)

    maintainer := "skiyooka@pm.me",

    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5", // May 2022
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11", // Mar 2022

    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0", // Apr 2022

    libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.0", // Apr 2024

    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test, // Sep 2022

    libraryDependencies += "org.scala-lang.modules" % "scala-swing_2.13" % "3.0.0", // May 2021
  )
