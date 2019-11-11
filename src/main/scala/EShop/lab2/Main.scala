package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CloseCheckout, RemoveItem}
import EShop.lab2.Checkout.{ReceivePayment, SelectDeliveryMethod, SelectPayment, StartCheckout}
import akka.actor.{ActorRef, ActorSystem, Props}

object Main extends App {
  val system        = ActorSystem("TESTING_SYSTEM")
  val cartActor     = system.actorOf(Props[CartActor], name = "cart")
  val checkoutActor = system.actorOf(Props[Checkout], name = "checkout")
  var checkout      = ActorRef.noSender

  cartActor ! AddItem("Overwatch")
  cartActor ! AddItem("Diablo")
  cartActor ! RemoveItem("Overwatch")

  checkoutActor ! StartCheckout
  checkoutActor ! SelectDeliveryMethod("Amazon_Prime")
  checkoutActor ! SelectPayment("BLIK")
  checkoutActor ! ReceivePayment

  cartActor ! CloseCheckout

  checkoutActor ! CloseCheckout
}
