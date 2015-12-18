package com.examples

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.log4j.Logger

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.StreamingContext._
import com.examples.TutorialHelper._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}


object MainExample {
  
  
  def write(uri: String, filePath: String, data: Array[Byte]) = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val path = new Path(filePath)
    val conf = new Configuration()
    conf.set("fs.defaultFS", uri)
    val fs = FileSystem.get(conf)
    val os = fs.create(path)
    os.write(data)
    fs.close()
  }
  
  def main(args: Array[String]) {
    
    // Checkpoint directory
    val checkpointDir = TutorialHelper.getCheckpointDirectory()

    // Configure Twitter credentials
    val apiKey = "k1RcQeAr1sH6kxadMJREx09N5"
    val apiSecret = "YivtaDZ8z3gJdtZaIPX3SSnhBEetNddHZR9K1ipvfWRXxWLzTd"
    val accessToken = "4360924697-ItIRkCCWkAPQY1qjMtWWHVraNoBX6phHqJ062M4"
    val accessTokenSecret = "3yPdIcdKPS7vKXynKhwya0fmC6bH8yguxGXtbZEoDISF6"
    TutorialHelper.configureTwitterCredentials(apiKey, apiSecret, accessToken, accessTokenSecret)

    // Your code goes here

    val ssc = new StreamingContext(new SparkConf(), Seconds(1))
    val tweets = TwitterUtils.createStream(ssc, None)
    //val statuses = tweets.map(status => status.getText())

    println("------------Sample JSON Tweets-------")
    
    //Comprobamos que los tweets esten relacionados con peliculas
    //val statusesFilm = tweets.filter(status => status.getText.toLowerCase.contains("film"))
    
    // Lista de generos clave 

    val gen = Array("action", "adventure", "animation", "children", "comedy", "crime", "documentary", "drama", "fantasy", "film-noir", "horror", "musical", "mystery", "romance", "sci-fi", "thriller", "war", "western")

    // Se filtran los generos a los tweets que son peliculas

    val statusesFilter = tweets.filter { status =>

      gen.exists { word => status.getText.toLowerCase.contains(word) }

    }
    
    //statusesFilter.print()
    
    //Almacenamos los tweets que son peliculas en el HDFS
    var count2 = 0L
    var sol = ""
    statusesFilter.foreachRDD((rdd, time) => {
      val count = rdd.count()
      println("Numero de tweets: "+count+" en el tiempo: "+time.milliseconds.toString)
      
      if (count > 0) {
        rdd.saveAsTextFile("hdfs://localhost:9000/tweetsfilter/tweets_" + time.milliseconds.toString)
        //solo le realizamos el incremento si el numero es mayor al anterior
        if (count2 > 0 && count2 < count){
          //calculo del incremento
          val res = ((count-count2)*100)/count2
          
          
          if (res > 30){
            sol = sol + "El stream del momento "+time.milliseconds.toString+ " tiene un imcremento del: "+res+"% \n"
            println(sol)
            count2 = count     
          }else{
            count2 = count
          }
        }else{
          count2 = count
        } 
      }
      write("hdfs://localhost:9000", "log.txt", sol.getBytes)
    })
    
    
    //rdd.saveAsTextFile("hdfs://localhost:9000/tweets/tweets_" + time.milliseconds.toString)

    
    //statuses.print()
    ssc.checkpoint(checkpointDir)

    ssc.start()
    ssc.awaitTermination()
    println("Incremento final: "+sol)
    

  }
}

/*
//ejemplo de inicio
object MainExample {

  def main(arg: Array[String]) {

    var logger = Logger.getLogger(this.getClass())

    if (arg.length < 2) {
      logger.error("=> wrong parameters number")
      System.err.println("Usage: MainExample <path-to-files> <output-path>")
      System.exit(1)
    }

    val jobName = "MainExample"

    val conf = new SparkConf().setAppName(jobName)
    val sc = new SparkContext(conf)

    val pathToFiles = arg(0)
    val outputPath = arg(1)

    logger.info("=> jobName \"" + jobName + "\"")
    logger.info("=> pathToFiles \"" + pathToFiles + "\"")

    val files = sc.textFile(pathToFiles)

    // do your work here
    val rowsWithoutSpaces = files.map(_.replaceAll(" ", ","))

    // and save the result
    rowsWithoutSpaces.saveAsTextFile(outputPath)

  }
}
*/