package EShop.lab2

import EShop.lab2.CartActor.CloseCheckout
import EShop.lab3.Payment
import akka.actor._
import akka.event._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {
  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ReceivePayment                      extends Command
  case object Expire                              extends Command

  sealed trait Event
  case object CheckOutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef)      extends Event
  case object CheckoutStarted                       extends Event
  case object CheckoutCancelled                     extends Event
  case class DeliveryMethodSelected(method: String) extends Event

  def props(cart: ActorRef) = Props(new Checkout(cart))
}

class Checkout(cartActor: ActorRef) extends Actor {
  import Checkout._

  private val scheduler                           = context.system.scheduler
  private val log                                 = Logging(context.system, this)
  implicit val executionContext: ExecutionContext = context.system.dispatcher

  val checkoutTimerDuration: FiniteDuration = 1.seconds
  val paymentTimerDuration: FiniteDuration  = 1.seconds
  def checkoutTimer: Cancellable            = scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)
  def paymentTimer: Cancellable             = scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)

  def receive: Receive = LoggingReceive.withLabel("receive") {
    case StartCheckout => context become selectingDelivery(checkoutTimer)
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive.withLabel("selectingDelivery") {
    case SelectDeliveryMethod(_) =>
      timer.cancel()
      context become selectingPaymentMethod(checkoutTimer)

    case ExpireCheckout | CancelCheckout =>
      timer.cancel()
      context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive.withLabel("selectingPaymentMethod") {
    case SelectPayment(method) =>
      timer.cancel()
      val paymentActor: ActorRef = context.system.actorOf(Payment.props(method, sender, self))
      sender ! PaymentStarted(paymentActor)
      context become processingPayment(paymentTimer)

    case ExpireCheckout | CancelCheckout =>
      timer.cancel()
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive.withLabel("processingPayment") {
    case ReceivePayment =>
      timer.cancel()
      cartActor ! CloseCheckout
      context become closed

    case ExpireCheckout | CancelCheckout =>
      timer.cancel()
      context become cancelled
  }

  def cancelled: Receive = {
    case _ =>
  }

  def closed: Receive = {
    case _ =>
  }
}
