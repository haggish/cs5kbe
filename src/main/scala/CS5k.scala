import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.Http
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol

case class CodeSet(name: String, description: String, codes: Set[Code])

case class Code(name: String, description: String, values: Set[String])

trait Protocols extends DefaultJsonProtocol {
  implicit val codeFormat = jsonFormat3(Code.apply)
  implicit val codesetFormat = jsonFormat3(CodeSet.apply)
}

object CS5k extends App with Protocols {
  implicit val system = ActorSystem()

  implicit def executor = system.dispatcher

  implicit val materializer = FlowMaterializer()

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  Http().bind(interface = config.getString("http.interface"), port =
    config.getInt("http.port")).startHandlingWith {
    logRequestResult("cs5k") {
      pathPrefix("codesets") {
        get {
          pathEndOrSingleSlash {
            complete {
              "get all codesets"
            }
          } ~ path(Segment) { cid =>
            complete {
              s"get codeset $cid"
            }
          }
        } ~ put {
          pathPrefix(Segment) { id =>
            pathEnd {
              entity(as[CodeSet]) { cs =>
                complete {
                  s"save or update codeset $id: $cs"
                }
              }
            } ~ path(Segment) { cid =>
              entity(as[Code]) { c =>
                complete {
                  s"save or update code $cid in codeset $id: $c"
                }
              }
            }
          }
        } ~ delete {
          pathEnd {
            complete {
              "delete all codesets"
            }
          } ~ pathPrefix(Segment) { id =>
            pathEndOrSingleSlash {
              complete {
                s"delete codeset $id"
              }
            } ~ path(Segment) { cid =>
              complete {
                s"delete code $cid in codeset $id"
              }
            }
          }
        }
      }
    }
  }
}
