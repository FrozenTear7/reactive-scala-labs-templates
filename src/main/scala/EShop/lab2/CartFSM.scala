package EShop.lab2

import EShop.lab2.CartActor._
import EShop.lab2.CartFSM.Status
import akka.actor.{LoggingFSM, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object CartFSM {
  object Status extends Enumeration {
    type Status = Value
    val Empty, NonEmpty, InCheckout = Value
  }

  def props() = Props(new CartFSM())
}

class CartFSM extends LoggingFSM[Status.Value, Cart] {
  import EShop.lab2.CartFSM.Status._

  // useful for debugging, see: https://doc.akka.io/docs/akka/current/fsm.html#rolling-event-log
  override def logDepth = 12

  val cartTimerDuration: FiniteDuration = 1 seconds

  startWith(Empty, Cart.empty)

  when(Empty) {
    case Event(AddItem(item), cart: Cart) =>
      val newCart = cart.addItem(item)
      goto(NonEmpty) using newCart
  }

  when(NonEmpty, stateTimeout = cartTimerDuration) {
    case Event(StateTimeout, _) => goto(Empty).using(Cart.empty)
    case Event(event: RemoveItem, cart: Cart) if cart.contains(event.item) && cart.size == 1 =>
      val newCart = cart.removeItem(event.item)
      goto(Empty) using newCart

    case Event(StartCheckout, cart: Cart) => goto(InCheckout) using cart

    case Event(event: AddItem, cart: Cart) =>
      val newCart = cart.addItem(event.item)
      stay using newCart
    case Event(event: RemoveItem, cart: Cart) if cart.contains(event.item) =>
      val newCart = cart.removeItem(event.item)
      stay using newCart
  }

  when(InCheckout) {
    case Event(CloseCheckout, _) => goto(Empty) using Cart.empty

    case Event(CancelCheckout, cart) => goto(NonEmpty) using cart
  }
}
