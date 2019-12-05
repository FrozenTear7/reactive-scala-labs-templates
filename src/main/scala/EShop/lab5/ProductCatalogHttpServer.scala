package EShop.lab5

import EShop.lab5.ProductCatalog.{GetItems, Items}
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.json.RootJsonFormat

import scala.concurrent.duration._

class ProductCatalogHttpServer(system: ActorSystem) extends HttpApp with SprayJsonSupport with JsonSupport {
  implicit val getItemsFormat: RootJsonFormat[GetItems]        = jsonFormat2(ProductCatalog.GetItems)
  implicit val itemFormat: RootJsonFormat[ProductCatalog.Item] = jsonFormat5(ProductCatalog.Item)
  implicit val itemsFormat: RootJsonFormat[Items]              = jsonFormat1(ProductCatalog.Items)

  import system.dispatcher
  implicit val timeout: Timeout = 5.seconds

  private val productCatalog =
    system.actorSelection("akka.tcp://ProductCatalog@127.0.0.1:2553/user/productcatalog").resolveOne()

  override protected def routes: Route = {
    path("items") {
      post {
        entity(as[GetItems]) { query =>
          complete {
            productCatalog.flatMap(actor => (actor ? query).mapTo[Items])
          }
        }
      }
    }
  }
}

object ProductCatalogHttpServer extends App {
  private val config = ConfigFactory.load()
  private val system = ActorSystem("ProductCatalogHttp", config.getConfig("catalogserver").withFallback(config))
  new ProductCatalogHttpServer(system).startServer("localhost", 8001)
}
