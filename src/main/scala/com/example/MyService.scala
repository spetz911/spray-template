package com.example

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import com.mongodb.casbah.Imports._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}


// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  case class WordItem(category: String, name: String, text: String)

  def buildWord(oo: DBObject) = {
    WordItem(
      oo.getAsOrElse[String]("category", "new"),
      oo.getAsOrElse[String]("name", "apple"),
      oo.getAsOrElse[String]("text", "apple translate")
    )
  }

  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val wordFormat = jsonFormat3(WordItem)
  }
  import MyJsonProtocol._

  val mongoClient = MongoClient("localhost", 27017)
  val db = mongoClient("test")
  val coll = db("words")

  val myRoute =
    path("api" / "books") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            val allDocs: Seq[WordItem] = coll.find().map(buildWord).toSeq
            allDocs
          }
        }
      }
    } ~
    pathPrefix("static") {
      getFromDirectory("/Users/oleg/WebstormProjects/natasha/")
    }
}
